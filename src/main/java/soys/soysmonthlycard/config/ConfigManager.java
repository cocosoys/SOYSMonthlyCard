package soys.soysmonthlycard.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import soys.soysmonthlycard.SOYSMonthlyCard;

/**
 * 主配置文件（config.yml）访问器。
 * <p>集中收敛所有配置读取，避免各模块散落硬编码的配置路径；存储后端与镜像策略亦由此统一读取。</p>
 */
public class ConfigManager {

    private final SOYSMonthlyCard plugin;
    private FileConfiguration config;

    public ConfigManager(SOYSMonthlyCard plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public FileConfiguration raw() {
        return config;
    }

    // ================================================================
    //  通用
    // ================================================================

    public boolean isDebug() {
        return config.getBoolean("settings.debug", false);
    }

    public String getLanguage() {
        return config.getString("settings.language", "zh_CN");
    }

    /** 每月几号（含）之后可领取 */
    public int getClaimDay() {
        return config.getInt("settings.claim-day", 1);
    }

    /** 玩家上线时是否自动尝试领取 */
    public boolean isAutoClaimOnJoin() {
        return config.getBoolean("settings.auto-claim-on-join", true);
    }

    /** 月份格式（Java DateTimeFormatter 模式，如 yyyy-MM） */
    public String getMonthFormat() {
        return config.getString("settings.month-format", "yyyy-MM");
    }

    /** 金币单位文案 */
    public String getUnitMoney() {
        return config.getString("settings.units.money", "金币");
    }

    /** 点券单位文案 */
    public String getUnitPoints() {
        return config.getString("settings.units.points", "点券");
    }

    /** 上线离线补发：可领取期间离线、上线时自动补发并提示 */
    public boolean isOfflineCompensation() {
        return config.getBoolean("settings.offline-compensation", true);
    }

    // ================================================================
    //  存储后端
    // ================================================================

    public boolean isBackendEnabled(String backendId) {
        return config.getBoolean("storage.backends." + backendId + ".enabled", false);
    }

    public ConfigurationSection getBackendSection(String backendId) {
        return config.getConfigurationSection("storage.backends." + backendId);
    }

    public boolean isMirrorEnabled() {
        return config.getBoolean("storage.mirror.enabled", true);
    }

    public boolean isMirrorAsync() {
        return config.getBoolean("storage.mirror.async", true);
    }

    public boolean isSyncOnStartup() {
        return config.getBoolean("storage.mirror.sync-on-startup", false);
    }
}
