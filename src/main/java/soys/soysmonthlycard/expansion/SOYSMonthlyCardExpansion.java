package soys.soysmonthlycard.expansion;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import soys.soysmonthlycard.SOYSMonthlyCard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 向 PlaceholderAPI 注册本插件占位符，标识符: soysmonthlycard
 *   %soysmonthlycard_can_claim%        玩家当前是否有可领取（未领）的档位
 *   %soysmonthlycard_claim_day%        每月可领取日
 *   %soysmonthlycard_claimed_tiers%    玩家本月已领取的档位（逗号分隔）
 *   %soysmonthlycard_claimed_<tier>%   指定档位本月是否已领取（true/false）
 *   %soysmonthlycard_days_until%       距离下次可领取的天数
 *   %soysmonthlycard_claimed_count%    本月已领取的玩家数量
 *   %soysmonthlycard_top%              本月领取排行榜（前 10，文本）
 *   %soysmonthlycard_money_unit%       金币单位文案
 *   %soysmonthlycard_points_unit%      点券单位文案
 */
public class SOYSMonthlyCardExpansion extends PlaceholderExpansion {

    private final SOYSMonthlyCard plugin;

    public SOYSMonthlyCardExpansion(SOYSMonthlyCard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "soysmonthlycard";
    }

    @Override
    public String getAuthor() {
        return "SOYS";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return null;
        }
        UUID uuid = player.getUniqueId();
        String p = params.toLowerCase();
        switch (p) {
            case "can_claim": {
                ConfigurationSection tiers = plugin.getRewards().getConfigurationSection("tiers");
                if (tiers == null) {
                    return "false";
                }
                for (String tierKey : tiers.getKeys(false)) {
                    String perm = tiers.getString(tierKey + ".permission", "");
                    if ((perm.isEmpty() || player.getPlayer() != null && player.getPlayer().hasPermission(perm))
                            && !plugin.getClaimManager().hasClaimed(uuid, tierKey)) {
                        return "true";
                    }
                }
                return "false";
            }
            case "claim_day":
                return String.valueOf(plugin.getConfigManager().getClaimDay());
            case "claimed_tiers": {
                ConfigurationSection tiers = plugin.getRewards().getConfigurationSection("tiers");
                List<String> claimed = new ArrayList<>();
                if (tiers != null) {
                    for (String tierKey : tiers.getKeys(false)) {
                        if (plugin.getClaimManager().hasClaimed(uuid, tierKey)) {
                            claimed.add(tierKey);
                        }
                    }
                }
                return String.join(", ", claimed);
            }
            case "days_until": {
                LocalDate now = LocalDate.now();
                return String.valueOf(plugin.getClaimManager().daysUntilNextClaimable(
                        plugin.getConfigManager().getClaimDay(), now.getDayOfMonth(), now.lengthOfMonth()));
            }
            case "claimed_count":
                return String.valueOf(plugin.getStorage().getClaimsThisMonth().size());
            case "top":
                return formatTop();
            case "money_unit":
                return plugin.getConfigManager().getUnitMoney();
            case "points_unit":
                return plugin.getConfigManager().getUnitPoints();
            default:
                if (p.startsWith("claimed_")) {
                    String tier = p.substring("claimed_".length());
                    return plugin.getClaimManager().hasClaimed(uuid, tier) ? "true" : "false";
                }
                return null;
        }
    }

    /** 排行榜文本（前 10 名，用于 PAPI 输出） */
    private String formatTop() {
        Map<UUID, List<String>> claims = plugin.getStorage().getClaimsThisMonth();
        if (claims.isEmpty()) {
            return plugin.getMessages().getString("top-empty", "暂无");
        }
        StringBuilder sb = new StringBuilder();
        int rank = 0;
        for (Map.Entry<UUID, List<String>> entry : claims.entrySet()) {
            if (rank >= 10) {
                break;
            }
            rank++;
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            String name = op.getName() != null ? op.getName() : entry.getKey().toString();
            sb.append(rank).append(". ").append(name)
                    .append("(").append(String.join(",", entry.getValue())).append(") ");
        }
        return sb.toString().trim();
    }
}
