package soys.soysmonthlycard.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import soys.soysmonthlycard.SOYSMonthlyCard;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 领取审计日志：以追加方式记录「谁在何时领取了哪一档位」，便于管理与排查。
 * 日志写入插件数据目录下的文件（默认 logs/claims.log），可通过 config.yml 的 settings.audit 关闭。
 */
public class AuditLogger {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SOYSMonthlyCard plugin;
    private final boolean enabled;
    private final File file;

    public AuditLogger(SOYSMonthlyCard plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfigManager().raw();
        this.enabled = config.getBoolean("settings.audit.enabled", true);
        String path = config.getString("settings.audit.file", "logs/claims.log");
        this.file = new File(plugin.getDataFolder(), path);
    }

    public static AuditLogger create(SOYSMonthlyCard plugin) {
        return new AuditLogger(plugin);
    }

    /** 记录一次领取 */
    public void logClaim(Player player, String tier, String month) {
        if (!enabled) {
            return;
        }
        String ts = LocalDateTime.now().format(TS);
        String line = "[" + ts + "] " + player.getName() + " (" + player.getUniqueId()
                + ") 领取档位=" + tier + " 月份=" + month + System.lineSeparator();
        synchronized (this) {
            try {
                File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                Files.write(file.toPath(), line.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                plugin.getLogger().warning("写入领取审计日志失败: " + e.getMessage());
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
