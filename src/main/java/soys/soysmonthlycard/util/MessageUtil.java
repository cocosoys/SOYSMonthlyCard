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

    /** 将 & 颜色符号转换为 Bukkit 颜色码 */
    public static String color(String s) {
        if (s == null) {
            return "";
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
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
