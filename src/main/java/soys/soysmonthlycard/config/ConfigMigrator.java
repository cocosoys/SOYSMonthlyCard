package soys.soysmonthlycard.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import soys.soysmonthlycard.SOYSMonthlyCard;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * 配置文件版本迁移：在 reload 时若 rewards.yml / message.yml 的 config-version 低于当前版本，
 * 则从插件 jar 内的默认文件补全缺失的配置项（不覆盖用户已有值），并写回 config-version。
 */
public final class ConfigMigrator {

    public static final int REWARDS_VERSION = 1;
    public static final int MESSAGE_VERSION = 1;

    private ConfigMigrator() {
    }

    /**
     * 迁移单个外部配置文件。
     *
     * @param plugin        插件实例（用于读取 jar 内默认资源）
     * @param file          磁盘上的配置文件
     * @param resourceName  jar 内默认文件名（用于读取完整默认配置）
     * @param currentVersion 当前插件支持的配置版本
     */
    public static void migrate(SOYSMonthlyCard plugin, File file, String resourceName, int currentVersion) {
        if (!file.exists()) {
            return; // 首次运行会由 saveResource 写入完整默认，无需迁移
        }
        FileConfiguration fileCfg = YamlConfiguration.loadConfiguration(file);
        int fileVer = fileCfg.getInt("config-version", 0);
        if (fileVer >= currentVersion) {
            return; // 已是最新
        }

        try (InputStream in = plugin.getResource(resourceName)) {
            if (in != null) {
                FileConfiguration defCfg = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8));
                addMissing(fileCfg, defCfg, "");
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "读取默认配置 " + resourceName + " 失败: " + e.getMessage(), e);
        }

        fileCfg.set("config-version", currentVersion);
        try {
            fileCfg.save(file);
            plugin.getLogger().info("已补全 " + file.getName() + " 缺失配置项至版本 " + currentVersion);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "写回 " + file.getName() + " 失败: " + e.getMessage(), e);
        }
    }

    /** 将 defaults 中存在但 target 中缺失的键补全到 target（递归处理嵌套段） */
    private static void addMissing(ConfigurationSection target, ConfigurationSection defaults, String prefix) {
        for (String key : defaults.getKeys(false)) {
            String full = prefix.isEmpty() ? key : prefix + "." + key;
            if (!target.contains(full)) {
                target.set(full, defaults.get(full));
            } else if (defaults.isConfigurationSection(key) && target.isConfigurationSection(full)) {
                addMissing(target.getConfigurationSection(full),
                        defaults.getConfigurationSection(key), full);
            }
        }
    }
}
