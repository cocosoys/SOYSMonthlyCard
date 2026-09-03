package soys.soysmonthlycard.storage;

import soys.soysmonthlycard.SOYSMonthlyCard;
import soys.soysmonthlycard.storage.impl.MysqlStorage;
import soys.soysmonthlycard.storage.impl.SqlStorage;
import soys.soysmonthlycard.storage.impl.SqliteStorage;
import soys.soysmonthlycard.storage.impl.YamlStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * 存储协调器（移植自 SOYSLinkTeam 的主辅存储模型）。
 * <p>
 * <b>主辅模型</b>：所有已启用的后端中优先级最高者（MYSQL &gt; SQLITE &gt; YAML）成为主存储，
 * 承担全部读操作；其余后端作为辅助存储，在写入时被镜像同步，充当热备份与降级方案。
 * </p>
 * <p>
 * <b>写入顺序</b>：异步写入被收敛到单线程执行器，保证同一玩家的写操作严格有序，
 * 避免并发写导致的数据错乱。
 * </p>
 * <p>
 * <b>内存缓存</b>：维护 {@code ConcurrentHashMap<UUID, ClaimRecord>} 缓存层，
 * 玩家上线时通过 {@link #preload(UUID)} 异步预加载，领取判定（{@link #loadClaims} /
 * {@link #getClaimedMonth}）优先命中缓存，避免主线程同步读数据库；
 * 写入（{@link #markClaimed} / {@link #saveClaimsAsync}）先同步更新缓存，再异步持久化。
 * </p>
 */
public class StorageManager {

    private final SOYSMonthlyCard plugin;

    /** 全部已成功初始化的后端 */
    private final Map<StorageType, DataStorage> storages = new EnumMap<>(StorageType.class);

    private DataStorage primary;
    private final List<DataStorage> secondaries = new ArrayList<>();

    /** 串行写入线程，保证写顺序 */
    private ExecutorService writeExecutor;

    /** 内存缓存：玩家 UUID -> 领取记录，避免主线程同步读数据库 */
    private final Map<UUID, ClaimRecord> cache = new ConcurrentHashMap<>();

    public StorageManager(SOYSMonthlyCard plugin) {
        this.plugin = plugin;
    }

    // ================================================================
    //  生命周期
    // ================================================================

    /**
     * 构建并初始化所有启用的后端，选出主存储。
     *
     * @throws IllegalStateException 没有任何后端可用时抛出
     */
    public void initialize() {
        shutdownInternal(false);
        cache.clear();

        this.writeExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "SOYSMonthlyCard-Storage");
            thread.setDaemon(true);
            return thread;
        });

        for (StorageType type : StorageType.values()) {
            if (!plugin.getConfigManager().isBackendEnabled(type.getId())) {
                continue;
            }
            DataStorage storage = buildStorage(type);
            if (storage == null) {
                continue;
            }
            try {
                storage.initialize();
                storages.put(type, storage);
                plugin.getLogger().info("已启用存储后端: " + type.getDisplayName()
                        + " (" + storage.describe() + ")");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE,
                        "存储后端 " + type.getDisplayName() + " 初始化失败，已跳过: " + e.getMessage(), e);
            }
        }

        if (storages.isEmpty()) {
            // 兼容旧版配置（未配置 storage.backends）：自动回退到 YAML 存储，避免插件崩溃
            plugin.getLogger().warning("未在 config.yml 的 storage.backends 中启用任何后端，自动回退到 YAML 存储。");
            try {
                YamlStorage fallback = new YamlStorage(plugin);
                fallback.initialize();
                storages.put(StorageType.YAML, fallback);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "没有任何可用的存储后端，请检查 config.yml 中 storage.backends 的配置", e);
            }
        }

        // 按优先级降序排序，最高者为主存储
        List<DataStorage> sorted = new ArrayList<>(storages.values());
        sorted.sort(Comparator.comparingInt((DataStorage s) -> s.getType().getPriority()).reversed());

        this.primary = sorted.get(0);
        this.secondaries.clear();
        for (int i = 1; i < sorted.size(); i++) {
            secondaries.add(sorted.get(i));
        }

        plugin.getLogger().info("主存储: " + primary.getType().getDisplayName()
                + (secondaries.isEmpty() ? "，无辅助存储" : "，辅助存储: " + describeSecondaries()));

        startKeepAliveTask();

        if (plugin.getConfigManager().isSyncOnStartup() && !secondaries.isEmpty()) {
            plugin.getLogger().info("正在执行启动时同步...");
            submit(() -> {
                try {
                    int count = syncToSecondaries();
                    plugin.getLogger().info("启动同步完成，已写入 " + count + " 名玩家");
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "启动同步失败: " + e.getMessage(), e);
                }
            });
        }
    }

    /**
     * 关闭所有后端，等待写队列排空。
     */
    public void shutdown() {
        shutdownInternal(true);
    }

    private void shutdownInternal(boolean awaitWrites) {
        if (writeExecutor != null) {
            writeExecutor.shutdown();
            if (awaitWrites) {
                try {
                    if (!writeExecutor.awaitTermination(15, TimeUnit.SECONDS)) {
                        plugin.getLogger().warning("存储写入队列未能在 15 秒内排空，部分数据可能丢失");
                        writeExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    writeExecutor.shutdownNow();
                }
            }
            writeExecutor = null;
        }
        for (DataStorage storage : storages.values()) {
            try {
                storage.shutdown();
            } catch (Exception e) {
                plugin.getLogger().warning("关闭存储后端 " + storage.getType().getId()
                        + " 时出错: " + e.getMessage());
            }
        }
        storages.clear();
        secondaries.clear();
        primary = null;
        cache.clear();
    }

    /**
     * 构建后端实例。新增后端类型时在此注册即可。
     */
    private DataStorage buildStorage(StorageType type) {
        switch (type) {
            case YAML:
                return new YamlStorage(plugin);
            case SQLITE:
                return new SqliteStorage(plugin);
            case MYSQL:
                return new MysqlStorage(plugin);
            default:
                return null;
        }
    }

    private void startKeepAliveTask() {
        DataStorage mysql = storages.get(StorageType.MYSQL);
        if (!(mysql instanceof MysqlStorage)) {
            return;
        }
        int seconds = ((MysqlStorage) mysql).getKeepAliveSeconds();
        if (seconds <= 0) {
            return;
        }
        long ticks = seconds * 20L;
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin,
                () -> ((SqlStorage) mysql).keepAlive(), ticks, ticks);
    }

    // ================================================================
    //  访问器
    // ================================================================

    public DataStorage getPrimary() {
        return primary;
    }

    public List<DataStorage> getSecondaries() {
        return Collections.unmodifiableList(secondaries);
    }

    public DataStorage getStorage(StorageType type) {
        return storages.get(type);
    }

    public Collection<DataStorage> getAllStorages() {
        return Collections.unmodifiableCollection(storages.values());
    }

    public boolean isEnabled(StorageType type) {
        return storages.containsKey(type);
    }

    private String describeSecondaries() {
        StringBuilder builder = new StringBuilder();
        for (DataStorage storage : secondaries) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(storage.getType().getDisplayName());
        }
        return builder.toString();
    }

    // ================================================================
    //  异步调度
    // ================================================================

    /**
     * 提交一个任务到串行写入线程。
     */
    public void submit(Runnable task) {
        ExecutorService executor = this.writeExecutor;
        if (executor == null || executor.isShutdown()) {
            // 关服阶段直接在当前线程执行，保证数据不丢
            task.run();
            return;
        }
        executor.submit(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "存储任务执行异常: " + t.getMessage(), t);
            }
        });
    }

    /**
     * 回到主线程执行。
     */
    private void sync(Runnable task) {
        if (!plugin.isEnabled()) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    // ================================================================
    //  读
    // ================================================================

    /**
     * 从缓存读取玩家领取记录；缓存未命中时从主存储加载并放入缓存。
     * <p>缓存命中时直接返回，不会阻塞主线程。</p>
     */
    public ClaimRecord loadClaims(UUID uuid) {
        ClaimRecord cached = cache.get(uuid);
        if (cached != null) {
            return new ClaimRecord(cached.getUuid(), cached.getTiers());
        }
        try {
            ClaimRecord record = primary.load(uuid);
            if (record != null) {
                cache.put(uuid, new ClaimRecord(record.getUuid(), record.getTiers()));
            }
            return record;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "读取 " + uuid + " 的领取记录失败: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 异步读取玩家领取记录，结果回调在主线程执行。
     */
    public void loadClaimsAsync(UUID uuid, Consumer<ClaimRecord> callback) {
        submit(() -> {
            ClaimRecord result = loadClaims(uuid);
            sync(() -> callback.accept(result));
        });
    }

    /**
     * 异步预加载玩家领取记录到缓存（玩家上线时调用，避免后续领取时阻塞主线程）。
     * 缓存已存在时直接返回，不重复加载。
     */
    public void preload(UUID uuid) {
        if (cache.containsKey(uuid)) {
            return;
        }
        submit(() -> {
            try {
                ClaimRecord record = primary.load(uuid);
                if (record != null) {
                    cache.put(uuid, new ClaimRecord(record.getUuid(), record.getTiers()));
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "预加载 " + uuid + " 的领取记录失败: " + e.getMessage(), e);
            }
        });
    }

    public Set<UUID> loadAllUuids() {
        try {
            return primary.loadAllUuids();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "读取全部玩家 UUID 失败: " + e.getMessage(), e);
            return Collections.emptySet();
        }
    }

    /** 读取某玩家所有档位的领取记录（tier -> month） */
    public Map<String, String> getAllClaims(UUID uuid) {
        ClaimRecord rec = loadClaims(uuid);
        return rec == null ? Collections.emptyMap() : rec.getTiers();
    }

    /** 有领取记录的玩家数量 */
    public int countRecords() {
        try {
            return primary.countRecords();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 读取本月已领取的玩家及其档位（tier 列表）。
     * 用于排行榜与 PAPI。可能在主线程遍历全部记录，数据量大时慎用。
     *
     * @return 玩家 UUID -> 本月已领取的档位名列表
     */
    public Map<UUID, List<String>> getClaimsThisMonth() {
        Set<UUID> uuids = loadAllUuids();
        String month = plugin.getClaimManager().currentMonth();
        Map<UUID, List<String>> out = new LinkedHashMap<>();
        for (UUID uuid : uuids) {
            ClaimRecord rec = loadClaims(uuid);
            if (rec == null) {
                continue;
            }
            List<String> tiers = new ArrayList<>();
            for (Map.Entry<String, String> entry : rec.getTiers().entrySet()) {
                if (month.equals(entry.getValue())) {
                    tiers.add(entry.getKey());
                }
            }
            if (!tiers.isEmpty()) {
                out.put(uuid, tiers);
            }
        }
        return out;
    }

    // ================================================================
    //  写
    // ================================================================

    /**
     * 异步保存记录：先更新缓存，再异步写入主存储并镜像到辅助存储。
     */
    public void saveClaimsAsync(ClaimRecord record) {
        if (record == null || record.getUuid() == null) {
            return;
        }
        cache.put(record.getUuid(), new ClaimRecord(record.getUuid(), record.getTiers()));
        submit(() -> saveClaimsBlocking(record));
    }

    /**
     * 同步保存记录（阻塞当前线程），关服流程使用。同时更新缓存。
     */
    public void saveClaimsBlocking(ClaimRecord record) {
        if (record == null || record.getUuid() == null) {
            return;
        }
        cache.put(record.getUuid(), new ClaimRecord(record.getUuid(), record.getTiers()));
        try {
            primary.save(record);
            debug("已保存 " + record.getUuid() + " 的领取记录到 " + primary.getType().getId());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE,
                    "保存领取记录到主存储失败: " + e.getMessage(), e);
            return;
        }
        mirror(storage -> storage.save(record), "保存 " + record.getUuid());
    }

    /**
     * 异步删除某玩家的领取记录。先清除缓存，再异步删除持久化数据。
     */
    public void deleteClaimsAsync(UUID uuid) {
        cache.remove(uuid);
        submit(() -> {
            try {
                primary.delete(uuid);
                debug("已从 " + primary.getType().getId() + " 删除 " + uuid);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "从主存储删除 " + uuid + " 失败: " + e.getMessage(), e);
                return;
            }
            mirror(storage -> storage.delete(uuid), "删除 " + uuid);
        });
    }

    /**
     * 标记某玩家某档位在指定月份已领取。
     * <p>先同步更新内存缓存（保证后续 hasClaimed 立即可见，且不阻塞主线程），
     * 再异步持久化到主存储并镜像到辅助存储。</p>
     */
    public void markClaimed(UUID uuid, String tier, String month) {
        // 1. 同步更新缓存（线程安全，compute 保证原子性）
        ClaimRecord updated = cache.compute(uuid, (k, existing) -> {
            ClaimRecord record = existing != null ? existing : new ClaimRecord(uuid);
            record.setTier(tier, month);
            return record;
        });
        // 2. 异步持久化（使用副本，避免缓存对象被并发修改）
        ClaimRecord toSave = new ClaimRecord(updated.getUuid(), updated.getTiers());
        submit(() -> {
            try {
                primary.save(toSave);
                debug("已标记领取 " + uuid + "/" + tier + " 到 " + primary.getType().getId());
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "主存储标记领取失败: " + e.getMessage(), e);
                return;
            }
            mirror(storage -> storage.save(toSave), "标记领取 " + uuid + "/" + tier);
        });
    }

    /**
     * 同步读取某玩家某档位已领取月份（未领取返回 null）。
     */
    public String getClaimedMonth(UUID uuid, String tier) {
        ClaimRecord rec = loadClaims(uuid);
        return rec == null ? null : rec.getTier(tier);
    }

    /**
     * 把一次写操作镜像到所有辅助存储。
     */
    private void mirror(StorageAction action, String description) {
        if (secondaries.isEmpty() || !plugin.getConfigManager().isMirrorEnabled()) {
            return;
        }
        Runnable task = () -> {
            for (DataStorage storage : secondaries) {
                if (!storage.isAvailable()) {
                    continue;
                }
                try {
                    action.execute(storage);
                } catch (Exception e) {
                    plugin.getLogger().warning("[镜像] " + description + " 写入 "
                            + storage.getType().getId() + " 失败: " + e.getMessage());
                }
            }
        };
        if (plugin.getConfigManager().isMirrorAsync()) {
            submit(task);
        } else {
            task.run();
        }
    }

    // ================================================================
    //  迁移与同步
    // ================================================================

    /**
     * 在两个后端之间迁移数据。
     *
     * @param from      来源后端
     * @param to        目标后端
     * @param overwrite true 表示先清空目标后端
     * @return 迁移的记录数量
     * @throws Exception 任一步骤失败
     */
    public int migrate(StorageType from, StorageType to, boolean overwrite) throws Exception {
        DataStorage source = storages.get(from);
        DataStorage target = storages.get(to);
        if (source == null) {
            throw new IllegalStateException("来源后端 " + from.getId() + " 未启用");
        }
        if (target == null) {
            throw new IllegalStateException("目标后端 " + to.getId() + " 未启用");
        }
        Collection<ClaimRecord> records = source.loadAll();
        if (overwrite) {
            target.clear();
        }
        target.saveAll(records);
        return records.size();
    }

    /**
     * 把主存储的全量数据覆盖同步到所有辅助存储。
     *
     * @return 同步的记录数量
     */
    public int syncToSecondaries() throws Exception {
        if (secondaries.isEmpty()) {
            return 0;
        }
        Collection<ClaimRecord> records = primary.loadAll();
        for (DataStorage storage : secondaries) {
            if (!storage.isAvailable()) {
                continue;
            }
            try {
                storage.clear();
                storage.saveAll(records);
            } catch (Exception e) {
                plugin.getLogger().warning("同步到 " + storage.getType().getId()
                        + " 失败: " + e.getMessage());
            }
        }
        return records.size();
    }

    private void debug(String message) {
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("[存储] " + message);
        }
    }

    /**
     * 可抛异常的存储操作，用于镜像写入。
     */
    @FunctionalInterface
    private interface StorageAction {
        void execute(DataStorage storage) throws Exception;
    }
}
