package soys.soysmonthlycard.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import soys.soysmonthlycard.SOYSMonthlyCard;
import soys.soysmonthlycard.api.HookResult;
import soys.soysmonthlycard.util.MessageUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 月卡领取核心逻辑：按 rewards.yml 中的权限档位判定可领取档位并发放奖励。
 */
public class ClaimManager {

    private final SOYSMonthlyCard plugin;

    /** 默认月份格式（config.yml 可覆盖） */
    public static final String DEFAULT_MONTH_PATTERN = "yyyy-MM";

    /**
     * 1.13+ 材质名到 1.12.2 的兼容映射。
     * 部分材质在 1.13 扁平化更新中被重命名，用户若从高版本教程复制配置，
     * 在此映射后可正常解析。新增映射时保持 key 为大写。
     */
    private static final java.util.Map<String, String> MATERIAL_COMPAT = new java.util.HashMap<>();
    static {
        MATERIAL_COMPAT.put("TOTEM_OF_UNDYING", "TOTEM");
    }

    public ClaimManager(SOYSMonthlyCard plugin) {
        this.plugin = plugin;
    }

    /** 当前年月，例如 2026-08（格式取自 config.yml 的 settings.month-format） */
    public String currentMonth() {
        return formatMonth(LocalDate.now(), plugin.getConfigManager().getMonthFormat());
    }

    /** 当前是否处于可领取时间段（当月日 >= 配置的 claim-day） */
    public boolean isClaimTime() {
        return isClaimable(plugin.getConfigManager().getClaimDay(), LocalDate.now().getDayOfMonth());
    }

    public boolean hasClaimed(UUID uuid, String tier) {
        String month = plugin.getStorage().getClaimedMonth(uuid, tier);
        return month != null && month.equals(currentMonth());
    }

    // ================================================================
    //  纯逻辑辅助方法（可在单元测试中直接调用，无需服务器）
    // ================================================================

    /** 判断指定日期是否处于可领取窗口：dayOfMonth >= claimDay */
    public static boolean isClaimable(int claimDay, int dayOfMonth) {
        return dayOfMonth >= claimDay;
    }

    /** 按指定格式格式化月份；格式非法时回退到 yyyy-MM */
    public static String formatMonth(LocalDate date, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            pattern = DEFAULT_MONTH_PATTERN;
        }
        try {
            return date.format(DateTimeFormatter.ofPattern(pattern));
        } catch (Exception e) {
            return date.format(DateTimeFormatter.ofPattern(DEFAULT_MONTH_PATTERN));
        }
    }

    /**
     * 距离下次可领取的天数。
     * - 若当前已在可领取窗口内（day >= claimDay），返回到「下个月 claimDay 日」的天数；
     * - 否则返回到「本月 claimDay 日」的天数。
     */
    public static int daysUntilNextClaimable(int claimDay, int dayOfMonth, int lengthOfMonth) {
        if (dayOfMonth >= claimDay) {
            return (lengthOfMonth - dayOfMonth) + claimDay;
        }
        return claimDay - dayOfMonth;
    }

    /** 从 tiers 配置中收集玩家拥有权限的档位（perm 为空视为对所有人开放） */
    public static List<String> collectEligibleTiers(ConfigurationSection tiers, Player player) {
        List<String> out = new ArrayList<>();
        if (tiers == null) {
            return out;
        }
        for (String tierKey : tiers.getKeys(false)) {
            ConfigurationSection tier = tiers.getConfigurationSection(tierKey);
            if (tier == null) {
                continue;
            }
            String perm = tier.getString("permission", "");
            if (perm.isEmpty() || player.hasPermission(perm)) {
                out.add(tierKey);
            }
        }
        return out;
    }

    // ================================================================
    //  领取流程
    // ================================================================

    /** 领取结果汇总 */
    public static class ClaimSummary {
        public ClaimResult result = ClaimResult.SUCCESS;
        public final List<String> eligibleTiers = new ArrayList<>();
        public final List<String> claimedTiers = new ArrayList<>();
        public final List<String> alreadyTiers = new ArrayList<>();
        public final List<String> blockedTiers = new ArrayList<>();
        public String blockReason = "";
    }

    public enum ClaimResult {
        SUCCESS, NO_PERMISSION, NOT_CLAIM_TIME, ALREADY_CLAIMED, BLOCKED
    }

    /**
     * 尝试为玩家领取其拥有权限的全部档位。
     * 多权限 / 权限继承：对所有拥有 permission 的档位逐一判定与发放。
     */
    public ClaimSummary tryClaim(Player player) {
        ClaimSummary summary = new ClaimSummary();
        ConfigurationSection tiers = plugin.getRewards().getConfigurationSection("tiers");
        if (tiers == null) {
            return summary;
        }

        boolean claimTime = isClaimTime();
        String month = currentMonth();
        UUID uuid = player.getUniqueId();

        for (String tierKey : tiers.getKeys(false)) {
            ConfigurationSection tier = tiers.getConfigurationSection(tierKey);
            if (tier == null) {
                continue;
            }
            String perm = tier.getString("permission", "");
            if (!perm.isEmpty() && !player.hasPermission(perm)) {
                continue; // 无该档位权限，跳过
            }
            summary.eligibleTiers.add(tierKey);

            if (!claimTime) {
                continue; // 未到领取时间，稍后统一报错
            }
            if (hasClaimed(uuid, tierKey)) {
                summary.alreadyTiers.add(tierKey);
                continue;
            }

            // 领取前钩子校验（供其他插件拦截/放行）
            HookResult hr = plugin.getApi().check(player, tierKey);
            if (!hr.isAllowed()) {
                summary.blockedTiers.add(tierKey);
                if (summary.blockReason.isEmpty()) {
                    summary.blockReason = hr.getReason();
                }
                continue;
            }

            // 发放该档位奖励
            grantTier(player, tier);
            plugin.getStorage().markClaimed(uuid, tierKey, month);
            plugin.getAuditLogger().logClaim(player, tierKey, month);
            summary.claimedTiers.add(tierKey);
        }

        // 判定结果
        if (summary.eligibleTiers.isEmpty()) {
            summary.result = ClaimResult.NO_PERMISSION;
        } else if (!summary.claimedTiers.isEmpty()) {
            summary.result = ClaimResult.SUCCESS;
        } else if (!summary.blockedTiers.isEmpty()) {
            summary.result = ClaimResult.BLOCKED;
        } else {
            summary.result = claimTime ? ClaimResult.ALREADY_CLAIMED : ClaimResult.NOT_CLAIM_TIME;
        }
        return summary;
    }

    /** 发放单个档位的奖励（money/points/items/commands） */
    private void grantTier(Player player, ConfigurationSection tier) {
        ConfigurationSection rw = tier.getConfigurationSection("rewards");
        if (rw == null) {
            return;
        }
        int money = rw.getInt("money", 0);
        int points = rw.getInt("points", 0);

        if (money > 0 && plugin.getEconomy() != null) {
            plugin.getEconomy().depositPlayer(player, money);
        }
        if (points > 0 && plugin.getPlayerPointsAPI() != null) {
            plugin.getPlayerPointsAPI().give(player.getUniqueId(), points);
        }
        for (String cmd : rw.getStringList("commands")) {
            if (cmd == null || cmd.isEmpty()) {
                continue;
            }
            String parsed = MessageUtil.parse(player, cmd);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
        List<ItemStack> items = parseItems(rw.getMapList("items"));
        if (!items.isEmpty()) {
            giveItems(player, items.toArray(new ItemStack[0]));
        }
    }

    /** 解析 items 列表为 ItemStack（支持 material/amount/data/name/lore/enchants） */
    private List<ItemStack> parseItems(List<Map<?, ?>> raw) {
        List<ItemStack> items = new ArrayList<>();
        if (raw == null) {
            return items;
        }
        for (Map<?, ?> map : raw) {
            String matName = (String) map.get("material");
            if (matName == null) {
                continue;
            }
            // 1.12.2 兼容：先查高版本材质名映射
            String compatName = MATERIAL_COMPAT.get(matName.toUpperCase());
            if (compatName != null) {
                matName = compatName;
            }
            Material material = Material.matchMaterial(matName);
            if (material == null) {
                plugin.getLogger().warning("rewards.yml 中存在未知材质: " + matName);
                continue;
            }
            int amount = 1;
            if (map.get("amount") != null) {
                try {
                    amount = Integer.parseInt(map.get("amount").toString());
                } catch (NumberFormatException ignore) {
                    amount = 1;
                }
            }
            ItemStack is = new ItemStack(material, Math.max(1, amount));
            if (map.get("data") != null) {
                try {
                    is.setDurability((short) Integer.parseInt(map.get("data").toString()));
                } catch (NumberFormatException ignore) {
                    // 忽略非法 data
                }
            }
            ItemMeta meta = is.getItemMeta();
            if (meta != null) {
                if (map.get("name") != null) {
                    meta.setDisplayName(MessageUtil.color(map.get("name").toString()));
                }
                if (map.get("lore") != null && map.get("lore") instanceof List) {
                    meta.setLore(MessageUtil.colorList((List<String>) map.get("lore")));
                }
                if (map.get("enchants") != null && map.get("enchants") instanceof List) {
                    for (Object e : (List<?>) map.get("enchants")) {
                        String[] parts = e.toString().split(":");
                        Enchantment ench = Enchantment.getByName(parts[0]);
                        if (ench != null) {
                            int level = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                            meta.addEnchant(ench, level, true);
                        }
                    }
                }
                is.setItemMeta(meta);
            }
            items.add(is);
        }
        return items;
    }

    /** 将物品发放到玩家背包，背包满则在脚下掉落 */
    private void giveItems(Player player, ItemStack[] items) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(items);
        for (ItemStack left : leftover.values()) {
            if (left != null && left.getAmount() > 0) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
        }
    }

    /**
     * 根据领取结果向玩家发送对应提示。
     *
     * @param silentIfNotSuccess 为 true 时仅在 SUCCESS 时提示（用于上线自动领取，避免刷屏）
     * @param auto               为 true 时表示由上线自动触发（使用离线补发文案）
     */
    public void handleResult(Player player, ClaimSummary summary, boolean silentIfNotSuccess, boolean auto) {
        if (silentIfNotSuccess && summary.result != ClaimResult.SUCCESS) {
            return;
        }
        org.bukkit.configuration.file.FileConfiguration msg = plugin.getMessages();
        switch (summary.result) {
            case SUCCESS: {
                String tiers = String.join(", ", summary.claimedTiers);
                String key = (auto && plugin.getConfigManager().isOfflineCompensation())
                        ? "offline-compensated" : "claimed";
                String m = msg.getString(key, "&a领取成功！已领取档位：&e%tiers%");
                m = m.replace("%tiers%", tiers);
                MessageUtil.tellParsed(player, m);
                String bc = msg.getString("claimed-broadcast", "");
                if (bc != null && !bc.isEmpty()) {
                    bc = bc.replace("%tiers%", tiers);
                    Bukkit.broadcastMessage(MessageUtil.parse(player, bc));
                }
                if (!summary.blockedTiers.isEmpty()) {
                    sendBlocked(player, summary);
                }
                break;
            }
            case NO_PERMISSION:
                MessageUtil.tell(player, msg.getString("no-permission", "&c无权限。"));
                break;
            case NOT_CLAIM_TIME: {
                String m = msg.getString("not-claimed-yet", "&c本月尚未到领取时间（每月 %claim_day% 号之后才可领取）。");
                m = m.replace("%claim_day%", String.valueOf(plugin.getConfigManager().getClaimDay()));
                MessageUtil.tellParsed(player, m);
                break;
            }
            case ALREADY_CLAIMED:
                MessageUtil.tell(player, msg.getString("already-claimed", "&c你本月可领取的月卡档位都已领取过了。"));
                break;
            case BLOCKED:
                sendBlocked(player, summary);
                break;
        }
    }

    /** 便捷重载：手动领取（非自动）时使用 */
    public void handleResult(Player player, ClaimSummary summary, boolean silentIfNotSuccess) {
        handleResult(player, summary, silentIfNotSuccess, false);
    }

    private void sendBlocked(Player player, ClaimSummary summary) {
        org.bukkit.configuration.file.FileConfiguration msg = plugin.getMessages();
        String m = msg.getString("claim-blocked", "&c档位 %tiers% 被拦截，无法领取：%reason%");
        m = m.replace("%tiers%", String.join(", ", summary.blockedTiers))
                .replace("%reason%", summary.blockReason);
        MessageUtil.tellParsed(player, m);
    }
}
