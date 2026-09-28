package soys.soysmonthlycard.web;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import soys.soysmonthlycard.manager.ClaimManager;
import soys.soysmonthlycard.util.MessageUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 物品双向序列化工具：在 Bukkit {@link ItemStack} 与 rewards.yml 的物品 Map 格式之间转换。
 *
 * <p>Map 字段（与 rewards.yml 完全一致）：</p>
 * <ul>
 *   <li>{@code material} : 材质名（必填，如 DIAMOND）</li>
 *   <li>{@code amount}   : 数量（默认 1）</li>
 *   <li>{@code data}     : 数据值/伤害值（可选）</li>
 *   <li>{@code name}     : 显示名（& 颜色码）</li>
 *   <li>{@code lore}     : Lore 列表（& 颜色码）</li>
 *   <li>{@code enchants} : 附魔列表（格式 附魔名:等级）</li>
 * </ul>
 */
public final class ItemSerializer {

    private ItemSerializer() {
    }

    /**
     * 将单个物品序列化为 rewards.yml 兼容的 Map。
     *
     * @param item 物品；null 或材质为 AIR 返回 null
     * @return 有序 Map（保持字段顺序，便于写入 yml 后阅读）
     */
    public static Map<String, Object> toMap(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("material", item.getType().name());
        if (item.getAmount() > 1) {
            map.put("amount", item.getAmount());
        }
        short durability = item.getDurability();
        if (durability != 0) {
            map.put("data", durability);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                map.put("name", decolor(meta.getDisplayName()));
            }
            if (meta.hasLore()) {
                List<String> lore = new ArrayList<>();
                for (String line : meta.getLore()) {
                    lore.add(decolor(line));
                }
                map.put("lore", lore);
            }
            if (meta.hasEnchants()) {
                List<String> enchants = new ArrayList<>();
                for (Map.Entry<Enchantment, Integer> e : meta.getEnchants().entrySet()) {
                    enchants.add(e.getKey().getName() + ":" + e.getValue());
                }
                map.put("enchants", enchants);
            }
        }
        return map;
    }

    /**
     * 将一批物品（如背包内容）序列化为 Map 列表，自动跳过空槽位。
     */
    public static List<Map<String, Object>> serializeAll(ItemStack[] items) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (items == null) {
            return out;
        }
        for (ItemStack item : items) {
            Map<String, Object> map = toMap(item);
            if (map != null) {
                out.add(map);
            }
        }
        return out;
    }

    /**
     * 将 rewards.yml 物品 Map 列表反序列化为物品列表。
     * 兼容两种来源：getMapList（Map）与 ConfigurationSection 读取。
     *
     * @param raw 原始 Map 列表
     * @param warningSink 未知材质时的警告输出（可为 null，静默跳过）
     */
    public static List<ItemStack> toItems(List<Map<?, ?>> raw, org.bukkit.plugin.Plugin warningSink) {
        List<ItemStack> items = new ArrayList<>();
        if (raw == null) {
            return items;
        }
        for (Map<?, ?> map : raw) {
            ItemStack item = toItemStack(map, warningSink);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    /**
     * 将单个物品 Map 反序列化为物品。
     *
     * @return 物品；材质缺失/未知返回 null
     */
    @SuppressWarnings("unchecked")
    public static ItemStack toItemStack(Map<?, ?> map, org.bukkit.plugin.Plugin warningSink) {
        if (map == null) {
            return null;
        }
        Object matObj = map.get("material");
        if (matObj == null) {
            return null;
        }
        String matName = matObj.toString();
        // 1.12.2 兼容：先查高版本材质名映射
        String compatName = ClaimManager.compatMaterial(matName.toUpperCase());
        if (compatName != null) {
            matName = compatName;
        }
        Material material = Material.matchMaterial(matName);
        if (material == null) {
            if (warningSink != null) {
                warningSink.getLogger().warning("存在未知材质: " + matName);
            }
            return null;
        }

        int amount = parseInt(map.get("amount"), 1);
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        if (map.get("data") != null) {
            item.setDurability((short) Math.max(0, parseInt(map.get("data"), 0)));
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Object name = map.get("name");
            if (name != null) {
                meta.setDisplayName(MessageUtil.color(name.toString()));
            }
            Object lore = map.get("lore");
            if (lore instanceof List) {
                meta.setLore(MessageUtil.colorList((List<String>) lore));
            }
            Object enchants = map.get("enchants");
            if (enchants instanceof List) {
                for (Object e : (List<?>) enchants) {
                    String[] parts = e.toString().split(":");
                    Enchantment ench = Enchantment.getByName(parts[0]);
                    if (ench != null) {
                        int level = parts.length > 1 ? parseInt(parts[1], 1) : 1;
                        meta.addEnchant(ench, level, true);
                    }
                }
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 从 ConfigurationSection 读取物品列表（用于直接读取 yml 配置）。
     */
    public static List<Map<String, Object>> readMaps(ConfigurationSection section, String key) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (section == null) {
            return out;
        }
        for (Map<?, ?> m : section.getMapList(key)) {
            Map<String, Object> converted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                converted.put(String.valueOf(e.getKey()), e.getValue());
            }
            out.add(converted);
        }
        return out;
    }

    /** 把 Bukkit 颜色码 § 还原为 & 形式，便于写入配置文件 */
    public static String decolor(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('§', '&');
    }

    private static int parseInt(Object obj, int def) {
        if (obj == null) {
            return def;
        }
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
