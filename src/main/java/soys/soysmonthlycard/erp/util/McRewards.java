package soys.soysmonthlycard.erp.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import soys.soysmonthlycard.SOYSMonthlyCard;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ERP 模块共用的 rewards.yml 读写与类型转换辅助（避免各 impl 重复）。
 */
public final class McRewards {

    private McRewards() {
    }

    /** tiers 配置段（不存在返回 null）。 */
    public static ConfigurationSection tiers(SOYSMonthlyCard plugin) {
        return plugin.getRewards().getConfigurationSection("tiers");
    }

    /** 指定档位配置段（不存在返回 null）。 */
    public static ConfigurationSection tier(SOYSMonthlyCard plugin, String tierKey) {
        return plugin.getRewards().getConfigurationSection("tiers." + tierKey);
    }

    /** rewards.yml 的可写视图（非 YamlConfiguration 时返回空实例兜底）。 */
    public static YamlConfiguration yaml(SOYSMonthlyCard plugin) {
        FileConfiguration cfg = plugin.getRewards();
        return cfg instanceof YamlConfiguration ? (YamlConfiguration) cfg : new YamlConfiguration();
    }

    /** 保存 rewards.yml 并重新加载，使领取逻辑立即使用新配置。 */
    public static void save(SOYSMonthlyCard plugin, YamlConfiguration rewards) throws Exception {
        rewards.save(new File(plugin.getDataFolder(), "rewards.yml"));
        plugin.reloadExternalFiles();
    }

    public static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    public static int toInt(Object o) {
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
    public static List<String> toStringList(Object o) {
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
