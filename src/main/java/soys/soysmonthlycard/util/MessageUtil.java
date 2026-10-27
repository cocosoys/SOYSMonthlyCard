package soys.soysmonthlycard.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息工具：颜色转换、PlaceholderAPI 占位符解析、发送。
 */
public class MessageUtil {

    /** 将 & 与 § 颜色符号转换为 Bukkit 颜色码（语言包规范为 § 直接书写，& 兼容旧文案/玩家输入） */
    public static String color(String s) {
        if (s == null) {
            return "";
        }
        String translated = org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
        return org.bukkit.ChatColor.translateAlternateColorCodes('§', translated);
    }

    /**
     * 占位符替换：{0} {1}...（与 SOYSHTTPOverMC I18n 占位符语法一致）。
     * 参数为 null 时跳过对应占位符。
     */
    public static String format(String template, Object... args) {
        if (template == null) {
            return "";
        }
        if (args == null || args.length == 0) {
            return template;
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                continue;
            }
            template = template.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return template;
    }

    /** 颜色 + PlaceholderAPI 占位符解析（player 为 null 时仅做颜色处理） */
    public static String parse(Player player, String s) {
        if (s == null) {
            return "";
        }
        String colored = color(s);
        if (player != null && isPlaceholderAPIPresent()) {
            colored = PlaceholderAPI.setPlaceholders(player, colored);
        }
        return colored;
    }

    public static void tell(CommandSender sender, String msg) {
        if (msg == null || msg.isEmpty()) {
            return;
        }
        sender.sendMessage(color(msg));
    }

    public static void tell(Player player, String msg) {
        if (msg == null || msg.isEmpty()) {
            return;
        }
        player.sendMessage(color(msg));
    }

    /** 批量颜色转换（用于 lore 等列表） */
    public static List<String> colorList(List<String> list) {
        List<String> out = new ArrayList<>();
        if (list == null) {
            return out;
        }
        for (String s : list) {
            out.add(color(s));
        }
        return out;
    }

    /** 颜色 + 占位符解析后发送给玩家 */
    public static void tellParsed(Player player, String msg) {
        if (msg == null || msg.isEmpty()) {
            return;
        }
        player.sendMessage(parse(player, msg));
    }

    private static boolean isPlaceholderAPIPresent() {
        return org.bukkit.Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }
}
