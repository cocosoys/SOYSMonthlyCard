package soys.soysmonthlycard.storage.impl;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import soys.soysmonthlycard.SOYSMonthlyCard;
import soys.soysmonthlycard.storage.ClaimRecord;
import soys.soysmonthlycard.storage.DataStorage;
import soys.soysmonthlycard.storage.StorageType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * YAML 文件存储后端（移植自 SOYSLinkTeam）。
 * <p>
 * 零外部依赖，默认启用。所有记录写在同一个 claims.yml 中，
 * 通过整对象加锁保证并发安全。适用于中小型服务器。
 * 结构：claims.&lt;uuid&gt;.tiers.&lt;tier&gt; = 月份(yyyy-MM)。
 * </p>
 */
public class YamlStorage implements DataStorage {

    private static final String ROOT = "claims";

    private final SOYSMonthlyCard plugin;
    private final Object lock = new Object();

    private File file;
    private YamlConfiguration config;
    private boolean available = false;
    private boolean backupOnSave = false;

    public YamlStorage(SOYSMonthlyCard plugin) {
        this.plugin = plugin;
    }

    @Override
    public StorageType getType() {
        return StorageType.YAML;
    }

    @Override
    public void initialize() throws Exception {
        ConfigurationSection section = plugin.getConfigManager().getBackendSection("yaml");
        String path = section == null ? "data/claims.yml" : section.getString("file", "data/claims.yml");
        this.backupOnSave = section != null && section.getBoolean("backup-on-save", false);

        this.file = new File(plugin.getDataFolder(), path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("无法创建数据目录: " + parent.getAbsolutePath());
        }
        if (!file.exists() && !file.createNewFile()) {
            throw new IOException("无法创建数据文件: " + file.getAbsolutePath());
        }
        synchronized (lock) {
            this.config = YamlConfiguration.loadConfiguration(file);
            if (!config.isConfigurationSection(ROOT)) {
                config.createSection(ROOT);
            }
        }
        this.available = true;
    }

    @Override
    public void shutdown() {
        synchronized (lock) {
            try {
                if (config != null && file != null) {
                    config.save(file);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("[YAML] 关闭时保存失败: " + e.getMessage());
            }
            available = false;
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String describe() {
        return file == null ? "未初始化" : file.getPath().replace('\\', '/');
    }

    // ================================================================
    //  读
    // ================================================================

    @Override
    public ClaimRecord load(UUID uuid) {
        synchronized (lock) {
            ConfigurationSection section = config.getConfigurationSection(ROOT + "." + uuid);
            return section == null ? null : deserialize(uuid, section);
        }
    }

    @Override
    public Collection<ClaimRecord> loadAll() {
        synchronized (lock) {
            Collection<ClaimRecord> list = new ArrayList<>();
            ConfigurationSection root = config.getConfigurationSection(ROOT);
            if (root == null) {
                return list;
            }
            for (String key : root.getKeys(false)) {
                UUID id = parseUuid(key);
                if (id == null) {
                    continue;
                }
                ConfigurationSection section = root.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                ClaimRecord rec = deserialize(id, section);
                if (rec != null) {
                    list.add(rec);
                }
            }
            return list;
        }
    }

    @Override
    public Set<UUID> loadAllUuids() {
        synchronized (lock) {
            Set<UUID> set = new HashSet<>();
            ConfigurationSection root = config.getConfigurationSection(ROOT);
            if (root == null) {
                return set;
            }
            for (String key : root.getKeys(false)) {
                UUID id = parseUuid(key);
                if (id != null) {
                    set.add(id);
                }
            }
            return set;
        }
    }

    @Override
    public int countRecords() {
        synchronized (lock) {
            ConfigurationSection root = config.getConfigurationSection(ROOT);
            return root == null ? 0 : root.getKeys(false).size();
        }
    }

    // ================================================================
    //  写
    // ================================================================

    @Override
    public void save(ClaimRecord record) throws Exception {
        synchronized (lock) {
            serialize(record);
            flush();
        }
    }

    @Override
    public void saveAll(Collection<ClaimRecord> records) throws Exception {
        synchronized (lock) {
            for (ClaimRecord record : records) {
                serialize(record);
            }
            flush();
        }
    }

    @Override
    public void delete(UUID uuid) throws Exception {
        synchronized (lock) {
            config.set(ROOT + "." + uuid, null);
            flush();
        }
    }

    @Override
    public void clear() throws Exception {
        synchronized (lock) {
            config.set(ROOT, null);
            config.createSection(ROOT);
            flush();
        }
    }

    // ================================================================
    //  内部
    // ================================================================

    private void flush() throws IOException {
        if (backupOnSave && file.exists()) {
            File backup = new File(file.getParentFile(), file.getName() + ".bak");
            try {
                Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                plugin.getLogger().warning("[YAML] 备份失败: " + e.getMessage());
            }
        }
        config.save(file);
    }

    private void serialize(ClaimRecord record) {
        String base = ROOT + "." + record.getUuid();
        config.set(base + ".tiers", new HashMap<>(record.getTiers()));
    }

    private ClaimRecord deserialize(UUID uuid, ConfigurationSection section) {
        ConfigurationSection tiers = section.getConfigurationSection("tiers");
        Map<String, String> map = new HashMap<>();
        if (tiers != null) {
            for (String key : tiers.getKeys(false)) {
                map.put(key, tiers.getString(key, ""));
            }
        }
        return new ClaimRecord(uuid, map);
    }

    private UUID parseUuid(String input) {
        if (input == null) {
            return null;
        }
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
