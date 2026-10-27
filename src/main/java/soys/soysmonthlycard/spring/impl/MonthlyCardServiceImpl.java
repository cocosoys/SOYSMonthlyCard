package soys.soysmonthlycard.spring.impl;

import com.github.cocosoys.mc.soyshttpovermc.util.JsonReader;
import com.github.cocosoys.mc.soyshttpovermc.web.ApiRequestContext;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import soys.soysmonthlycard.SOYSMonthlyCard;
import soys.soysmonthlycard.exception.WebException;
import soys.soysmonthlycard.spring.entity.vo.InventoryVO;
import soys.soysmonthlycard.spring.entity.vo.LogViewVO;
import soys.soysmonthlycard.spring.entity.vo.OverviewVO;
import soys.soysmonthlycard.spring.entity.vo.PlayerListVO;
import soys.soysmonthlycard.spring.entity.vo.PlayerStateVO;
import soys.soysmonthlycard.spring.entity.vo.PlayerTierStateVO;
import soys.soysmonthlycard.spring.entity.vo.RewardsVO;
import soys.soysmonthlycard.spring.entity.vo.SettingsViewVO;
import soys.soysmonthlycard.spring.entity.vo.TierDetailVO;
import soys.soysmonthlycard.spring.entity.vo.TierListRowVO;
import soys.soysmonthlycard.spring.entity.vo.TierListViewVO;
import soys.soysmonthlycard.spring.entity.vo.TierRecordVO;
import soys.soysmonthlycard.spring.entity.vo.TierRecordsVO;
import soys.soysmonthlycard.spring.entity.vo.TierToggleVO;
import soys.soysmonthlycard.spring.entity.vo.TierViewVO;
import soys.soysmonthlycard.spring.service.MonthlyCardService;
import soys.soysmonthlycard.storage.ClaimRecord;
import soys.soysmonthlycard.web.ItemSerializer;
import soys.soysmonthlycard.web.Sync;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * {@link MonthlyCardService} 的实现。
 *
 * <p>集中承载原控制器的全部业务逻辑、鉴权门禁、文件读写、存储访问与主线程调度；
 * 控制器瘦身后仅保留取参与响应包装。</p>
 */
public class MonthlyCardServiceImpl implements MonthlyCardService {

    private final SOYSMonthlyCard plugin;

    public MonthlyCardServiceImpl(SOYSMonthlyCard plugin) {
        this.plugin = plugin;
    }

    // ================================================================
    //  鉴权门禁（原控制器 denyIfNotOp）
    // ================================================================

    /**
     * 校验当前请求来自在线 OP；不满足时抛出 {@link WebException}。
     *
     * <p>未认证/凭证无效 → {@link WebException.Kind#UNAUTHORIZED}；
     * 不在线或非 OP → {@link WebException.Kind#FORBIDDEN}。</p>
     */
    private void assertOp(ApiRequestContext ctx) {
        String name = ctx == null ? null : ctx.getPlayerName();
        if (name == null || name.isEmpty()) {
            throw new WebException(WebException.Kind.UNAUTHORIZED, "未认证或凭证无效，无法识别操作者");
        }
        Player online = ctx.getSyncPlayer();
        if (online == null) {
            throw new WebException(WebException.Kind.FORBIDDEN,
                    "仅限在线 OP 操作：凭证玩家 " + name + " 当前不在服务器内");
        }
        if (!online.isOp()) {
            throw new WebException(WebException.Kind.FORBIDDEN,
                    "无权限：仅 OP 可操作（当前: " + name + "）");
        }
    }

    // ================================================================
    //  用户侧
    // ================================================================

    @Override
    public TierListViewVO viewTiers() {
        List<TierViewVO> out = new ArrayList<>();
        ConfigurationSection tiers = plugin.getRewards().getConfigurationSection("tiers");
        if (tiers != null) {
            for (String key : tiers.getKeys(false)) {
                ConfigurationSection tier = tiers.getConfigurationSection(key);
                if (tier == null || !tier.getBoolean("enabled", true)) {
                    continue;
                }
                out.add(buildTierView(key, tier));
            }
        }
        TierListViewVO vo = new TierListViewVO();
        vo.setMonth(plugin.getClaimManager().currentMonth());
        vo.setClaimDay(plugin.getConfigManager().getClaimDay());
        vo.setClaimTime(plugin.getClaimManager().isClaimTime());
        vo.setTiers(out);
        return vo;
    }

    @Override
    public PlayerStateVO playerState(ApiRequestContext ctx) {
        PlayerStateVO vo = new PlayerStateVO();
        String name = ctx.getPlayerName();
        vo.setName(name);
        Map<String, PlayerTierStateVO> tiers = new LinkedHashMap<>();
        if (name != null) {
            Player online = ctx.getSyncPlayer();
            UUID uuid = online != null ? online.getUniqueId() : null;
            ConfigurationSection all = plugin.getRewards().getConfigurationSection("tiers");
            if (all != null) {
                for (String key : all.getKeys(false)) {
                    ConfigurationSection tier = all.getConfigurationSection(key);
                    PlayerTierStateVO info = new PlayerTierStateVO();
                    boolean claimed = uuid != null && plugin.getClaimManager().hasClaimed(uuid, key);
                    String perm = tier == null ? "" : tier.getString("permission", "");
                    boolean permitted = online == null || perm.isEmpty() || online.hasPermission(perm);
                    info.setClaimed(claimed);
                    info.setPermitted(permitted);
                    tiers.put(key, info);
                }
            }
        }
        vo.setTiers(tiers);
        return vo;
    }

    // ================================================================
    //  管理侧：概览 / 列表
    // ================================================================

    @Override
    public OverviewVO overview(ApiRequestContext ctx) {
        assertOp(ctx);
        Map<UUID, List<String>> claimedMonth = plugin.getStorage().getClaimsThisMonth();
        int totalClaims = 0;
        for (List<String> ts : claimedMonth.values()) {
            totalClaims += ts.size();
        }
        OverviewVO vo = new OverviewVO();
        vo.setOnlinePlayers(Bukkit.getOnlinePlayers().size());
        vo.setRecordedPlayers(plugin.getStorage().countRecords());
        vo.setTiersCount(tierKeys().size());
        vo.setClaimedThisMonth(totalClaims);
        vo.setCurrentMonth(plugin.getClaimManager().currentMonth());
        vo.setClaimTime(plugin.getClaimManager().isClaimTime());
        vo.setPrimaryStorage(plugin.getStorage().getPrimary().getType().getDisplayName());
        vo.setAuditEnabled(plugin.getAuditLogger().isEnabled());
        return vo;
    }

    @Override
    public List<TierListRowVO> listTiers(ApiRequestContext ctx) {
        assertOp(ctx);
        // 本月领取统计（一次遍历）
        Map<UUID, List<String>> claimedMonth = plugin.getStorage().getClaimsThisMonth();
        List<TierListRowVO> out = new ArrayList<>();
        ConfigurationSection tiers = plugin.getRewards().getConfigurationSection("tiers");
        if (tiers != null) {
            for (String key : tiers.getKeys(false)) {
                ConfigurationSection tier = tiers.getConfigurationSection(key);
                if (tier == null) {
                    continue;
                }
                ConfigurationSection rw = tier.getConfigurationSection("rewards");
                TierListRowVO row = new TierListRowVO();
                row.setKey(key);
                row.setPermission(tier.getString("permission", ""));
                row.setEnabled(tier.getBoolean("enabled", true));
                row.setMoney(rw == null ? 0 : rw.getInt("money", 0));
                row.setPoints(rw == null ? 0 : rw.getInt("points", 0));
                row.setCommandsCount(rw == null ? 0 : rw.getStringList("commands").size());
                row.setItemsCount(rw == null ? 0 : ItemSerializer.readMaps(rw, "items").size());
                int claimed = 0;
                for (List<String> ts : claimedMonth.values()) {
                    if (ts.contains(key)) {
                        claimed++;
                    }
                }
                row.setClaimedThisMonth(claimed);
                out.add(row);
            }
        }
        return out;
    }

    // ================================================================
    //  管理侧：档位详情 / 保存 / 启停
    // ================================================================

    @Override
    public TierDetailVO getTier(ApiRequestContext ctx, String tierKey) {
        assertOp(ctx);
        ConfigurationSection tier = tierSection(tierKey);
        if (tier == null) {
            throw new WebException(WebException.Kind.NOT_FOUND, "档位不存在: " + tierKey);
        }
        return buildTierDetail(tierKey, tier);
    }

    @Override
    public String saveTier(ApiRequestContext ctx, String tierKey, String body) {
        assertOp(ctx);
        ConfigurationSection existing = tierSection(tierKey);
        if (existing == null) {
            throw new WebException(WebException.Kind.NOT_FOUND, "档位不存在: " + tierKey);
        }
        Map<String, Object> payload = JsonReader.parseObject(body);
        try {
            YamlConfiguration rewards = rewardsYaml();
            String base = "tiers." + tierKey;
            if (payload.containsKey("permission")) {
                rewards.set(base + ".permission", str(payload.get("permission")));
            }
            if (payload.containsKey("enabled")) {
                rewards.set(base + ".enabled", payload.get("enabled"));
            }
            if (payload.containsKey("money")) {
                rewards.set(base + ".rewards.money", toInt(payload.get("money")));
            }
            if (payload.containsKey("points")) {
                rewards.set(base + ".rewards.points", toInt(payload.get("points")));
            }
            if (payload.containsKey("commands")) {
                rewards.set(base + ".rewards.commands", toStringList(payload.get("commands")));
            }
            if (payload.containsKey("items")) {
                Object items = payload.get("items");
                rewards.set(base + ".rewards.items", items instanceof List ? items : new ArrayList<>());
            }
            saveRewards(rewards);
            return "已保存档位 " + tierKey;
        } catch (Exception e) {
            throw new WebException(WebException.Kind.ERROR, "保存失败: " + e.getMessage());
        }
    }

    @Override
    public TierToggleVO toggleTier(ApiRequestContext ctx, String tierKey) {
        assertOp(ctx);
        ConfigurationSection tier = tierSection(tierKey);
        if (tier == null) {
            throw new WebException(WebException.Kind.NOT_FOUND, "档位不存在: " + tierKey);
        }
        boolean now = !tier.getBoolean("enabled", true);
        try {
            YamlConfiguration rewards = rewardsYaml();
            rewards.set("tiers." + tierKey + ".enabled", now);
            saveRewards(rewards);
        } catch (Exception e) {
            throw new WebException(WebException.Kind.ERROR, "切换失败: " + e.getMessage());
        }
        TierToggleVO vo = new TierToggleVO();
        vo.setEnabled(now);
        return vo;
    }

    // ================================================================
    //  管理侧：代领 / 重置 / 记录
    // ================================================================

    @Override
    public String claimFor(ApiRequestContext ctx, String tierKey, String body) {
        assertOp(ctx);
        Map<String, Object> payload = JsonReader.parseObject(body);
        String targetName = str(payload.get("player"));
        if (targetName == null || targetName.isEmpty()) {
            throw new WebException(WebException.Kind.ERROR, "请指定玩家名");
        }
        final String fName = targetName;
        // 领取涉及 Bukkit API，切回主线程
        String error = Sync.run(plugin, () -> {
            Player target = Bukkit.getPlayerExact(fName);
            if (target == null) {
                return "玩家不在线，无法代领: " + fName;
            }
            return plugin.getClaimManager().claimSingle(target, tierKey);
        });
        if (error != null) {
            throw new WebException(WebException.Kind.ERROR, error);
        }
        return "已为 " + fName + " 领取档位 " + tierKey;
    }

    @Override
    public String resetFor(ApiRequestContext ctx, String tierKey, String body) {
        assertOp(ctx);
        Map<String, Object> payload = JsonReader.parseObject(body);
        String targetName = str(payload.get("player"));
        if (targetName == null || targetName.isEmpty()) {
            throw new WebException(WebException.Kind.ERROR, "请指定玩家名");
        }
        OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
        boolean removed = plugin.getStorage().resetTier(off.getUniqueId(), tierKey);
        if (!removed) {
            throw new WebException(WebException.Kind.ERROR,
                    targetName + " 本就没有档位 " + tierKey + " 的领取记录");
        }
        return "已重置 " + targetName + " 的档位 " + tierKey + " 领取记录";
    }

    @Override
    public TierRecordsVO tierRecords(ApiRequestContext ctx, String tierKey) {
        assertOp(ctx);
        List<TierRecordVO> records = new ArrayList<>();
        Set<UUID> uuids = plugin.getStorage().loadAllUuids();
        for (UUID uuid : uuids) {
            ClaimRecord rec = plugin.getStorage().loadClaims(uuid);
            if (rec == null || rec.getTier(tierKey) == null) {
                continue;
            }
            TierRecordVO row = new TierRecordVO();
            row.setUuid(uuid.toString());
            row.setName(Bukkit.getOfflinePlayer(uuid).getName());
            row.setMonth(rec.getTier(tierKey));
            records.add(row);
        }
        records.sort(Comparator.comparing(TierRecordVO::getMonth,
                Comparator.nullsLast(Comparator.naturalOrder())));
        TierRecordsVO vo = new TierRecordsVO();
        vo.setTier(tierKey);
        vo.setTotal(records.size());
        vo.setRecords(records);
        return vo;
    }

    // ================================================================
    //  管理侧：设置
    // ================================================================

    @Override
    public SettingsViewVO getSettings(ApiRequestContext ctx) {
        assertOp(ctx);
        FileConfiguration cfg = plugin.getConfig();
        SettingsViewVO vo = new SettingsViewVO();
        ConfigurationSection settings = cfg.getConfigurationSection("settings");
        vo.setSettings(settings == null ? Collections.emptyMap() : settings.getValues(true));
        // 存储后端开关（不回传数据库密码等敏感连接信息）
        Map<String, Object> backends = new LinkedHashMap<>();
        ConfigurationSection bs = cfg.getConfigurationSection("storage.backends");
        if (bs != null) {
            for (String id : bs.getKeys(false)) {
                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("enabled", bs.getConfigurationSection(id).getBoolean("enabled", false));
                backends.put(id, meta);
            }
        }
        vo.setBackends(backends);
        ConfigurationSection mirror = cfg.getConfigurationSection("storage.mirror");
        vo.setMirror(mirror == null ? Collections.emptyMap() : mirror.getValues(true));
        return vo;
    }

    @Override
    public String saveSettings(ApiRequestContext ctx, String body) {
        assertOp(ctx);
        Map<String, Object> payload = JsonReader.parseObject(body);
        try {
            // 基础设置白名单
            String[] allowed = {
                    "claim-day", "auto-claim-on-join", "month-format",
                    "offline-compensation", "debug", "audit.enabled"
            };
            Object settings = payload.get("settings");
            if (settings instanceof Map) {
                Map<?, ?> sm = (Map<?, ?>) settings;
                for (String key : allowed) {
                    if (sm.containsKey(key)) {
                        plugin.getConfig().set("settings." + key, sm.get(key));
                    }
                }
                if (sm.containsKey("units") && sm.get("units") instanceof Map) {
                    Map<?, ?> units = (Map<?, ?>) sm.get("units");
                    if (units.get("money") != null) {
                        plugin.getConfig().set("settings.units.money", str(units.get("money")));
                    }
                    if (units.get("points") != null) {
                        plugin.getConfig().set("settings.units.points", str(units.get("points")));
                    }
                }
            }
            // 存储后端开关
            Object backends = payload.get("backends");
            if (backends instanceof Map) {
                for (Map.Entry<?, ?> e : ((Map<?, ?>) backends).entrySet()) {
                    String id = str(e.getKey());
                    if (e.getValue() instanceof Map && ((Map<?, ?>) e.getValue()).containsKey("enabled")) {
                        Object en = ((Map<?, ?>) e.getValue()).get("enabled");
                        plugin.getConfig().set("storage.backends." + id + ".enabled", en);
                    }
                }
            }
            // 镜像设置
            Object mirror = payload.get("mirror");
            if (mirror instanceof Map) {
                Map<?, ?> mm = (Map<?, ?>) mirror;
                for (Map.Entry<?, ?> e : mm.entrySet()) {
                    plugin.getConfig().set("storage.mirror." + str(e.getKey()), e.getValue());
                }
            }
            plugin.saveConfig();
            plugin.getConfigManager().reload();
            plugin.reloadAuditLogger();
            return "设置已保存（存储后端变更需重启服务器生效）";
        } catch (Exception e) {
            throw new WebException(WebException.Kind.ERROR, "保存设置失败: " + e.getMessage());
        }
    }

    // ================================================================
    //  管理侧：日志 / 背包 / 玩家
    // ================================================================

    @Override
    public LogViewVO getLogs(ApiRequestContext ctx) {
        assertOp(ctx);
        String path = plugin.getConfig().getString("settings.audit.file", "logs/claims.log");
        File file = new File(plugin.getDataFolder(), path);
        List<String> lines = new ArrayList<>();
        if (file.exists()) {
            try {
                List<String> all = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                int from = Math.max(0, all.size() - 500);
                lines = all.subList(from, all.size());
            } catch (Exception e) {
                throw new WebException(WebException.Kind.ERROR, "读取日志失败: " + e.getMessage());
            }
        }
        LogViewVO vo = new LogViewVO();
        vo.setEnabled(plugin.getAuditLogger().isEnabled());
        vo.setFile(path);
        vo.setLines(lines);
        return vo;
    }

    @Override
    public InventoryVO getInventory(ApiRequestContext ctx) {
        assertOp(ctx);
        Player operator = ctx.getSyncPlayer();
        if (operator == null) {
            throw new WebException(WebException.Kind.ERROR, "管理员不在线，无法读取背包");
        }
        // 背包读取切主线程（getSyncPlayer 已在主线程取过，这里内容读取同样需主线程）
        List<Map<String, Object>> contents = Sync.run(plugin,
                () -> ItemSerializer.serializeAll(operator.getInventory().getContents()));
        List<Map<String, Object>> hotbar = Sync.run(plugin,
                () -> ItemSerializer.serializeAll(operator.getInventory().getStorageContents()));
        InventoryVO vo = new InventoryVO();
        vo.setOperator(operator.getName());
        vo.setItems(contents);
        vo.setStorage(hotbar);
        return vo;
    }

    @Override
    public List<PlayerListVO> onlinePlayers(ApiRequestContext ctx) {
        assertOp(ctx);
        List<PlayerListVO> out = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerListVO m = new PlayerListVO();
            m.setName(p.getName());
            m.setUuid(p.getUniqueId().toString());
            m.setOp(p.isOp());
            out.add(m);
        }
        return out;
    }

    // ================================================================
    //  辅助方法（原控制器私有方法，随下沉迁入 impl）
    // ================================================================

    private List<String> tierKeys() {
        ConfigurationSection tiers = plugin.getRewards().getConfigurationSection("tiers");
        return tiers == null ? Collections.<String>emptyList() : new ArrayList<>(tiers.getKeys(false));
    }

    private ConfigurationSection tierSection(String tierKey) {
        return plugin.getRewards().getConfigurationSection("tiers." + tierKey);
    }

    /** 用户侧档位展示 VO。 */
    private TierViewVO buildTierView(String key, ConfigurationSection tier) {
        ConfigurationSection rw = tier.getConfigurationSection("rewards");
        TierViewVO vo = new TierViewVO();
        vo.setKey(key);
        vo.setMoney(rw == null ? 0 : rw.getInt("money", 0));
        vo.setPoints(rw == null ? 0 : rw.getInt("points", 0));
        vo.setCommands(rw == null ? Collections.<String>emptyList() : rw.getStringList("commands"));
        vo.setItems(rw == null ? Collections.<Map<String, Object>>emptyList()
                : ItemSerializer.readMaps(rw, "items"));
        return vo;
    }

    /** 管理侧档位完整详情 VO。 */
    private TierDetailVO buildTierDetail(String key, ConfigurationSection tier) {
        ConfigurationSection rw = tier.getConfigurationSection("rewards");
        TierDetailVO vo = new TierDetailVO();
        vo.setKey(key);
        vo.setPermission(tier.getString("permission", ""));
        vo.setEnabled(tier.getBoolean("enabled", true));
        RewardsVO rewards = new RewardsVO();
        rewards.setMoney(rw == null ? 0 : rw.getInt("money", 0));
        rewards.setPoints(rw == null ? 0 : rw.getInt("points", 0));
        rewards.setCommands(rw == null ? Collections.<String>emptyList() : rw.getStringList("commands"));
        rewards.setItems(rw == null ? Collections.<Map<String, Object>>emptyList()
                : ItemSerializer.readMaps(rw, "items"));
        vo.setRewards(rewards);
        return vo;
    }

    private YamlConfiguration rewardsYaml() {
        FileConfiguration cfg = plugin.getRewards();
        return cfg instanceof YamlConfiguration ? (YamlConfiguration) cfg : new YamlConfiguration();
    }

    /** 保存 rewards.yml 并重新加载，使领取逻辑立即使用新配置。 */
    private void saveRewards(YamlConfiguration rewards) throws Exception {
        rewards.save(new File(plugin.getDataFolder(), "rewards.yml"));
        plugin.reloadExternalFiles();
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static int toInt(Object o) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(o).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> toStringList(Object o) {
        if (o instanceof List) {
            List<String> out = new ArrayList<>();
            for (Object e : (List<Object>) o) {
                out.add(String.valueOf(e));
            }
            return out;
        }
        return new ArrayList<>();
    }
}
