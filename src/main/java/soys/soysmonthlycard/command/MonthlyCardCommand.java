package soys.soysmonthlycard.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import soys.soysmonthlycard.SOYSMonthlyCard;
import soys.soysmonthlycard.manager.ClaimManager;
import soys.soysmonthlycard.storage.StorageManager;
import soys.soysmonthlycard.storage.StorageType;
import soys.soysmonthlycard.util.MessageUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * /monthlycard —— 领取月卡礼包 / 查询 / 管理
 * 子指令:
 *   (无)       领取当前可领档位
 *   info       查看自己的各档位领取状态
 *   top        查看本月已领取排行榜
 *   reload     重载配置与存储（需管理员）
 *   migrate    数据迁移（如 mysql2yaml / yaml2mysql / sqlite2mysql，需管理员）
 * 别名: /syk /syueka /sgiftloft（兼容旧命令名）
 */
public class MonthlyCardCommand implements CommandExecutor, TabCompleter {

    private final SOYSMonthlyCard plugin;
    private final ClaimManager claimManager;

    public MonthlyCardCommand(SOYSMonthlyCard plugin, ClaimManager claimManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1) {
            String sub = args[0].toLowerCase();
            if (sub.equals("reload")) {
                if (!sender.hasPermission("soysmonthlycard.admin")) {
                    MessageUtil.tell(sender, plugin.getMessages().getString("soysmonthlycard.common.no-permission", "§c无权限。"));
                    return true;
                }
                plugin.getConfigManager().reload();
                plugin.reloadConfigFiles();
                plugin.reloadAuditLogger();
                plugin.getStorage().initialize();
                MessageUtil.tell(sender, plugin.getMessages().getString("soysmonthlycard.common.plugin-reload", "§a配置已重载。"));
                return true;
            }
            if (sub.equals("migrate")) {
                return handleMigrate(sender, args);
            }
            if (sub.equals("info")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("该指令只能由玩家执行。");
                    return true;
                }
                showInfo((Player) sender);
                return true;
            }
            if (sub.equals("top")) {
                showTop(sender);
                return true;
            }
        }

        // 领取 / 查询（仅玩家可执行）
        if (!(sender instanceof Player)) {
            sender.sendMessage("该指令只能由玩家执行。");
            return true;
        }
        Player player = (Player) sender;
        claimManager.handleResult(player, claimManager.tryClaim(player), false);
        return true;
    }

    /** 查看自己的各档位领取状态 */
    private void showInfo(Player player) {
        FileConfiguration msg = plugin.getMessages();
        FileConfiguration rewards = plugin.getRewards();
        String month = claimManager.currentMonth();
        int claimDay = plugin.getConfigManager().getClaimDay();

        MessageUtil.tell(player, msg.getString("soysmonthlycard.command.info.header", "§a===== 我的月卡领取状态 ====="));
        MessageUtil.tell(player, MessageUtil.format(
                msg.getString("soysmonthlycard.command.info.month", "§7当前月份: §f{0}"), month));
        MessageUtil.tell(player, MessageUtil.format(
                msg.getString("soysmonthlycard.command.info.claim-day", "§7每月可领取日: §f{0}§7 号起"),
                claimDay));

        org.bukkit.configuration.ConfigurationSection tiers = rewards.getConfigurationSection("tiers");
        if (tiers != null) {
            for (String tierKey : tiers.getKeys(false)) {
                String perm = tiers.getString(tierKey + ".permission", "");
                boolean hasPerm = perm.isEmpty() || player.hasPermission(perm);
                boolean claimed = claimManager.hasClaimed(player.getUniqueId(), tierKey);
                String last = plugin.getStorage().getClaimedMonth(player.getUniqueId(), tierKey);

                String status;
                if (!hasPerm) {
                    status = msg.getString("soysmonthlycard.command.info.status.no-permission", "§c无权限");
                } else if (claimed) {
                    status = msg.getString("soysmonthlycard.command.info.status.claimed", "§a已领取");
                } else if (claimManager.isClaimTime()) {
                    status = msg.getString("soysmonthlycard.command.info.status.available", "§e可领取");
                } else {
                    status = msg.getString("soysmonthlycard.command.info.status.locked", "§7未到时间");
                }

                String line = MessageUtil.format(
                        msg.getString("soysmonthlycard.command.info.tier", "§7- §f{0}§7: {1} §7(上次: {2})"),
                        tierKey, status, last == null ? "-" : last);
                MessageUtil.tell(player, line);
            }
        }
        MessageUtil.tell(player, msg.getString("soysmonthlycard.command.info.note", "§7使用 /monthlycard 领取"));
    }

    /** 查看本月已领取排行榜 */
    private void showTop(CommandSender sender) {
        FileConfiguration msg = plugin.getMessages();
        StorageManager storage = plugin.getStorage();
        Map<UUID, List<String>> claims = storage.getClaimsThisMonth();

        MessageUtil.tell(sender, MessageUtil.format(
                msg.getString("soysmonthlycard.command.top.header", "§a===== 本月月卡领取排行榜 (共 §f{0}§a 人) ====="),
                claims.size()));

        if (claims.isEmpty()) {
            MessageUtil.tell(sender, msg.getString("soysmonthlycard.command.top.empty", "§7暂无玩家领取。"));
            return;
        }

        int rank = 0;
        for (Map.Entry<UUID, List<String>> entry : claims.entrySet()) {
            rank++;
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            String name = op.getName() != null ? op.getName() : entry.getKey().toString();
            String line = MessageUtil.format(
                    msg.getString("soysmonthlycard.command.top.line", "§7#{0} §f{1} §7档位: {2}"),
                    rank, name, String.join(", ", entry.getValue()));
            MessageUtil.tell(sender, line);
        }
    }

    private boolean handleMigrate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("soysmonthlycard.admin")) {
            MessageUtil.tell(sender, plugin.getMessages().getString("soysmonthlycard.common.no-permission", "§c无权限。"));
            return true;
        }
        if (args.length < 2) {
            MessageUtil.tell(sender, "§c用法: /monthlycard migrate <mysql2yaml|yaml2mysql|sqlite2mysql|...>");
            return true;
        }
        String dir = args[1].toLowerCase();
        int sep = dir.indexOf('2');
        if (sep <= 0 || sep == dir.length() - 1) {
            MessageUtil.tell(sender, MessageUtil.format(
                    plugin.getMessages().getString("soysmonthlycard.command.migrate.unknown", "§c未知迁移方向: {0}"),
                    dir));
            return true;
        }
        String fromId = dir.substring(0, sep);
        String toId = dir.substring(sep + 1);
        StorageType from = StorageType.fromId(fromId);
        StorageType to = StorageType.fromId(toId);
        if (from == null || to == null || from == to) {
            MessageUtil.tell(sender, MessageUtil.format(
                    plugin.getMessages().getString("soysmonthlycard.command.migrate.unknown", "§c未知迁移方向: {0}"),
                    dir));
            return true;
        }

        StorageManager storage = plugin.getStorage();
        if (!storage.isEnabled(from) || !storage.isEnabled(to)) {
            MessageUtil.tell(sender, MessageUtil.format(
                    plugin.getMessages().getString("soysmonthlycard.command.migrate.backend-disabled",
                            "§c存储后端 {0} 未启用，无法执行迁移。"),
                    from.getDisplayName() + " / " + to.getDisplayName()));
            return true;
        }

        FileConfiguration msg = plugin.getMessages();
        MessageUtil.tell(sender, MessageUtil.format(
                msg.getString("soysmonthlycard.command.migrate.start", "§a开始迁移数据：从 {0} 到 {1} …"),
                from.getDisplayName(), to.getDisplayName()));

        int count;
        try {
            // overwrite=true：先清空目标后端再写入，保证目标与来源完全一致
            count = storage.migrate(from, to, true);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "数据迁移失败", e);
            MessageUtil.tell(sender, "§c迁移失败: " + e.getMessage());
            return true;
        }
        MessageUtil.tell(sender, MessageUtil.format(
                msg.getString("soysmonthlycard.command.migrate.done", "§a数据迁移完成：共处理 §f{0}§a 名玩家。"),
                count));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> opts = new ArrayList<>(Arrays.asList("info", "top"));
            if (sender.hasPermission("soysmonthlycard.admin")) {
                opts.addAll(Arrays.asList("reload", "migrate"));
            }
            return opts;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("migrate")
                && sender.hasPermission("soysmonthlycard.admin")) {
            return new ArrayList<>(Arrays.asList("mysql2yaml", "yaml2mysql", "sqlite2mysql", "mysql2sqlite", "yaml2sqlite"));
        }
        return Collections.emptyList();
    }
}
