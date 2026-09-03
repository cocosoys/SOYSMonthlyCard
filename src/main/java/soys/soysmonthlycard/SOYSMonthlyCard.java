package soys.soysmonthlycard;

import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import soys.soysmonthlycard.api.SOYSMonthlyCardAPI;
import soys.soysmonthlycard.command.MonthlyCardCommand;
import soys.soysmonthlycard.config.ConfigManager;
import soys.soysmonthlycard.config.ConfigMigrator;
import soys.soysmonthlycard.expansion.SOYSMonthlyCardExpansion;
import soys.soysmonthlycard.listener.JoinListener;
import soys.soysmonthlycard.manager.ClaimManager;
import soys.soysmonthlycard.storage.StorageManager;
import soys.soysmonthlycard.util.AuditLogger;

import java.io.File;
import java.util.logging.Level;

public final class SOYSMonthlyCard extends JavaPlugin {

    private static SOYSMonthlyCard instance;
    private Economy economy;
    private PlayerPointsAPI playerPointsAPI;
    private ConfigManager configManager;
    private ClaimManager claimManager;
    private StorageManager storageManager;
    private SOYSMonthlyCardAPI api;
    private AuditLogger auditLogger;

    private FileConfiguration rewardsConfig;
    private FileConfiguration messagesConfig;

    @Override
    public void onEnable() {
        instance = this;

        // 保存默认配置文件（仅首次）
        saveDefaultConfig();
        saveResourceIfAbsent("rewards.yml");
        saveResourceIfAbsent("message.yml");

        // 配置访问器（含 Bukkit config.yml 重载）
        this.configManager = new ConfigManager(this);
        // 加载 rewards.yml / message.yml
        reloadConfigFiles();

        // 三大强制依赖初始化
        if (!setupEconomy()) {
            getLogger().severe("未找到 Vault 或未注册 Economy 服务，SOYSMonthlyCard 已禁用！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (!setupPlayerPoints()) {
            getLogger().severe("未找到 PlayerPoints 插件，SOYSMonthlyCard 已禁用！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (!setupPlaceholderAPI()) {
            getLogger().severe("未找到 PlaceholderAPI 插件，SOYSMonthlyCard 已禁用！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 存储（主辅模型：MYSQL > SQLITE > YAML，主存储承担读，其余镜像写入）
        this.storageManager = new StorageManager(this);
        try {
            storageManager.initialize();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "存储初始化失败，SOYSMonthlyCard 已禁用: " + e.getMessage(), e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 领取逻辑
        this.claimManager = new ClaimManager(this);

        // 对外 API（领取前钩子注册入口）
        this.api = new SOYSMonthlyCardAPI(this);

        // 领取审计日志
        this.auditLogger = AuditLogger.create(this);

        // 注册监听器与指令
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        MonthlyCardCommand cmd = new MonthlyCardCommand(this, claimManager);
        getCommand("monthlycard").setExecutor(cmd);
        getCommand("monthlycard").setTabCompleter(cmd);

        getLogger().info("SOYSMonthlyCard 已启用 | 月卡领取日: 每月 "
                + configManager.getClaimDay() + " 号起 | 主存储: "
                + storageManager.getPrimary().getType().getDisplayName());
    }

    @Override
    public void onDisable() {
        if (storageManager != null) {
            storageManager.shutdown();
        }
        getLogger().info("SOYSMonthlyCard 已禁用。");
    }

    /** 重新加载外部配置文件（rewards.yml / message.yml），并自动补全缺失配置项 */
    public void reloadConfigFiles() {
        ConfigMigrator.migrate(this, new File(getDataFolder(), "rewards.yml"),
                "rewards.yml", ConfigMigrator.REWARDS_VERSION);
        ConfigMigrator.migrate(this, new File(getDataFolder(), "message.yml"),
                "message.yml", ConfigMigrator.MESSAGE_VERSION);
        reloadExternalFiles();
    }

    /** 重新加载外部配置文件（rewards.yml / message.yml） */
    public void reloadExternalFiles() {
        rewardsConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "rewards.yml"));
        messagesConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "message.yml"));
    }

    /** 重载后重建审计日志（使 settings.audit 配置立即生效） */
    public void reloadAuditLogger() {
        this.auditLogger = AuditLogger.create(this);
    }

    /** 若资源文件不存在则写入默认（不覆盖已有文件） */
    private void saveResourceIfAbsent(String name) {
        File file = new File(getDataFolder(), name);
        if (!file.exists()) {
            saveResource(name, false);
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private boolean setupPlayerPoints() {
        if (getServer().getPluginManager().getPlugin("PlayerPoints") == null) {
            return false;
        }
        PlayerPoints pp = (PlayerPoints) getServer().getPluginManager().getPlugin("PlayerPoints");
        playerPointsAPI = pp.getAPI();
        return playerPointsAPI != null;
    }

    private boolean setupPlaceholderAPI() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return false;
        }
        new SOYSMonthlyCardExpansion(this).register();
        return true;
    }

    public static SOYSMonthlyCard getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public PlayerPointsAPI getPlayerPointsAPI() {
        return playerPointsAPI;
    }

    public ClaimManager getClaimManager() {
        return claimManager;
    }

    public StorageManager getStorage() {
        return storageManager;
    }

    public SOYSMonthlyCardAPI getApi() {
        return api;
    }

    public AuditLogger getAuditLogger() {
        return auditLogger;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public FileConfiguration getRewards() {
        return rewardsConfig;
    }

    public FileConfiguration getMessages() {
        return messagesConfig;
    }
}
