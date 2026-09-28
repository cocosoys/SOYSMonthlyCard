package soys.soysmonthlycard.web;

import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPublic;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PathVariable;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PostMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PutMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestBody;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.JsonReader;
import com.github.cocosoys.mc.soyshttpovermc.web.ApiRequestContext;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import soys.soysmonthlycard.SOYSMonthlyCard;
import soys.soysmonthlycard.storage.ClaimRecord;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * SOYSMonthlyCard 业务端点。
 *
 * <p>端点统一挂 {@code /api/plugins/soysmonthlycard} 前缀：</p>
 *
 * <b>用户侧（凭证玩家，展示自身可领礼包）</b>
 * <ul>
 *   <li>{@code GET /view/tiers}    展示用月卡档位（仅启用，含物品/金币/点券）</li>
 *   <li>{@code GET /player/state}  当前玩家各档位本月领取状态</li>
 * </ul>
 *
 * <b>管理侧（仅在线 OP，ERP 后台）</b>
 * <ul>
 *   <li>{@code GET  /admin/overview}            概览统计</li>
 *   <li>{@code GET  /admin/tiers}               礼包档位列表</li>
 *   <li>{@code GET  /admin/tiers/{tier}}         档位完整配置（含物品）</li>
 *   <li>{@code PUT  /admin/tiers/{tier}}         保存档位配置与物品</li>
 *   <li>{@code POST /admin/tiers/{tier}/toggle}  启用/禁用</li>
 *   <li>{@code POST /admin/tiers/{tier}/claim}   代在线玩家领取</li>
 *   <li>{@code POST /admin/tiers/{tier}/reset}   重置玩家领取记录</li>
 *   <li>{@code GET  /admin/tiers/{tier}/records} 该档位领取记录</li>
 *   <li>{@code GET  /admin/settings}            读取设置</li>
 *   <li>{@code PUT  /admin/settings}            保存设置</li>
 *   <li>{@code GET  /admin/logs}                审计日志</li>
 *   <li>{@code GET  /admin/inventory}           当前管理员背包（拷贝物品用）</li>
 *   <li>{@code GET  /admin/players}             在线玩家列表</li>
 * </ul>
 */
public class MonthlyCardController {

    private final SOYSMonthlyCard plugin;

    public MonthlyCardController(SOYSMonthlyCard plugin) {
        this.plugin = plugin;
    }

    // ================================================================
    //  用户侧
    // ================================================================

    @ApiName("展示月卡档位")
    @ApiPublic
    @GetMapping("/view/tiers")
    public AjaxResult viewTiers() {
        List<Map<String, Object>> out = new ArrayList<>();
        ConfigurationSection tiers = plugin.getRewards().getConfigurationSection("tiers");
        if (tiers != null) {
            for (String key : tiers.getKeys(false)) {
                ConfigurationSection tier = tiers.getConfigurationSection(key);
                if (tier == null || !tier.getBoolean("enabled", true)) {
                    continue;
                }
                Map<String, Object> dto = tierView(key, tier);
                out.add(dto);
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("month", plugin.getClaimManager().currentMonth());
        data.put("claimDay", plugin.getConfigManager().getClaimDay());
        data.put("claimTime", plugin.getClaimManager().isClaimTime());
        data.put("tiers", out);
        return AjaxResult.success(data);
    }

    @ApiName("玩家领取状态")
    @ApiPublic
    @GetMapping("/player/state")
    public AjaxResult playerState(ApiRequestContext ctx) {
        Map<String, Object> data = new LinkedHashMap<>();
        String name = ctx.getPlayerName();
        data.put("name", name);
        Map<String, Object> tiers = new LinkedHashMap<>();
        if (name != null) {
            Player online = ctx.getSyncPlayer();
            UUID uuid = online != null ? online.getUniqueId() : null;
            ConfigurationSection all = plugin.getRewards().getConfigurationSection("tiers");
            if (all != null) {
                for (String key : all.getKeys(false)) {
                    Map<String, Object> info = new LinkedHashMap<>();
                    boolean claimed = uuid != null && plugin.getClaimManager().hasClaimed(uuid, key);
                    boolean perm = online == null
                            || all.getConfigurationSection(key).getString("permission", "").isEmpty()
                            || online.hasPermission(all.getConfigurationSection(key).getString("permission", ""));
                    info.put("claimed", claimed);
                    info.put("permitted", perm);
                    tiers.put(key, info);
                }
            }
        }
        data.put("tiers", tiers);
        return AjaxResult.success(data);
    }

    // ================================================================
    //  管理侧：概览 / 列表
    // ================================================================

    @ApiName("概览统计")
    @ApiPublic
    @GetMapping("/admin/overview")
    public AjaxResult overview(ApiRequestContext ctx) {
        AjaxResult deny = denyIfNotOp(ctx);
        if (deny != null) {
            return deny;
        }
        Map<UUID, List<String>> claimedMonth = plugin.getStorage().getClaimsThisMonth();
        int totalClaims = 0;
        for (List<String> tiers : claimedMonth.values()) {
            totalClaims += tiers.size();
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("onlinePlayers", Bukkit.getOnlinePlayers().size());
        data.put("recordedPlayers", plugin.getStorage().countRecords());
        data.put("tiersCount", tierKeys().size());
        data.put("claimedThisMonth", totalClaims);
        data.put("currentMonth", plugin.getClaimManager().currentMonth());
        data.put("claimTime", plugin.getClaimManager().isClaimTime());
        data.put("primaryStorage", plugin.getStorage().getPrimary().getType().getDisplayName());
        data.put("auditEnabled", plugin.getAuditLogger().isEnabled());
        return AjaxResult.success(data);
    }

    @ApiName("礼包档位列表")
    @ApiPublic
    @GetMapping("/admin/tiers")
    public AjaxResult listTiers(ApiRequestContext ctx) {
        AjaxResult deny = denyIfNotOp(ctx);
        if (deny != null) {
            return deny;
        }
        // 本月领取统计（一次遍历）
        Map<UUID, List<String>> claimedMonth = plugin.getStorage().getClaimsThisMonth();

        List<Map<String, Object>> out = new ArrayList<>();
        ConfigurationSection tiers = plugin.getRewards().getConfigurationSection("tiers");
        if (tiers != null) {
            for (String key : tiers.getKeys(false)) {
                ConfigurationSection tier = tiers.getConfigurationSection(key);
                if (tier == null) {
                    continue;
                }
                ConfigurationSection rw = tier.getConfigurationSection("rewards");
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("key", key);
                row.put("permission", tier.getString("permission", ""));
                row.put("enabled", tier.getBoolean("enabled", true));
                row.put("money", rw == null ? 0 : rw.getInt("money", 0));
                row.put("points", rw == null ? 0 : rw.getInt("points", 0));
                row.put("commandsCount", rw == null ? 0 : rw.getStringList("commands").size());
                row.put("itemsCount", rw == null ? 0 : rw.getMapList("items").size());
                int claimed = 0;
                for (List<String> ts : claimedMonth.values()) {
                    if (ts.contains(key)) {
                        claimed++;
                    }
                }
                row.put("claimedThisMonth", claimed);
                out.add(row);
            }
        }
        return AjaxResult.success(out);
    }

    // ================================================================
    //  管理侧：档位详情 / 保存 / 启停
    // ================================================================

    @ApiName("档位完整配置")
    @ApiPublic
    @GetMapping("/admin/tiers/{tier}")
    public AjaxResult getTier(ApiRequestContext ctx, @PathVariable(name = "tier") String tierKey) {
        AjaxResult deny = denyIfNotOp(ctx);
        if (deny != null) {
            return deny;
        }
        ConfigurationSection tier = tierSection(tierKey);
        if (tier == null) {
            return AjaxResult.notFound("档位不存在: " + tierKey);
        }
        return AjaxResult.success(tierDetail(tierKey, tier));
    }

    @ApiName("保存档位配置")
    @ApiPublic
    @PutMapping("/admin/tiers/{tier}")
    public AjaxResult saveTier(ApiRequestContext ctx, @PathVariable(name = "tier") String tierKey,
                               @RequestBody String body) {
        AjaxResult deny = denyIfNotOp(ctx);
        if (deny != null) {
            return deny;
        }
        ConfigurationSection existing = tierSection(tierKey);
        if (existing == null) {
            return AjaxResult.notFound("档位不存在: " + tierKey);
        }
        Map<String, Object> payload = JsonReader.parseObject(body);

        try {
            YamlConfiguration rewards = rewardsYaml();
            String base = "tiers." + tierKey;
            if (payload.containsKey("permission")) {
                rewards.set(base + ".permission", str(payload.get("permission")));
            }
            if (payload.containsKey("enabled")) {
                rewards.set(base + ".enabled", payload.get("enabled"));
            }
            if (payload.containsKey("money")) {
                rewards.set(base + ".rewards.money", toInt(payload.get("money")));
            }
            if (payload.containsKey("points")) {
                rewards.set(base + ".rewards.points", toInt(payload.get("points")));
            }
            if (payload.containsKey("commands")) {
                rewards.set(base + ".rewards.commands", toStringList(payload.get("commands")));
            }
            if (payload.containsKey("items")) {
                Object items = payload.get("items");
                rewards.set(base + ".rewards.items", items instanceof List ? items : new ArrayList<>());
            }
            saveRewards(rewards);
            return AjaxResult.success("已保存档位 " + tierKey);
        } catch (Exception e) {
            return AjaxResult.error("保存失败: " + e.getMessage());
        }
    }

    @ApiName("启用/禁用档位")
    @ApiPublic
    @PostMapping("/admin/tiers/{tier}/toggle")
    public AjaxResult toggleTier(ApiRequestContext ctx, @PathVariable(name = "tier") String tierKey) {
        AjaxResult deny = denyIfNotOp(ctx);
        if (deny != null) {
            return deny;
        }
        ConfigurationSection tier = tierSection(tierKey);
        if (tier == null) {
            return AjaxResult.notFound("档位不存在: " + tierKey);
        }
        boolean now = !tier.getBoolean("enabled", true);
        try {
            YamlConfiguration rewards = rewardsYaml();
            rewards.set("tiers." + tierKey + ".enabled", now);
            saveRewards(rewards);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("enabled", now);
            return AjaxResult.success(now ? "已启用档位 " + tierKey : "已禁用档位 " + tierKey, data);
        } catch (Exception e) {
            return AjaxResult.error("切换失败: " + e.getMessage());
        }
    }

    // ================================================================
    //  管理侧：代领 / 重置 / 记录
    // ================================================================

    @ApiName("代玩家领取")
    @ApiPublic
    @PostMapping("/admin/tiers/{tier}/claim")
    public AjaxResult claimFor(ApiRequestContext ctx, @PathVariable(name = "tier") String tierKey,
                               @RequestBody String body) {
        AjaxResult deny = denyIfNotOp(ctx);
        if (deny != null) {
            return deny;
        }
        String targetName = str(JsonReader.parseObject(body).get("player"));
        if (targetName == null || targetName.isEmpty()) {
            return AjaxResult.error("请指定玩家名");
        }
        final String fName = targetName;
        // 领取涉及 Bukkit API，切回主线程
        String error = Sync.run(plugin, () -> {
            Player target = Bukkit.getPlayerExact(fName);
            if (target == null) {
                return "玩家不在线，无法代领: " + fName;
            }
            return plugin.getClaimManager().claimSingle(target, tierKey);
        });
        if (error != null) {
            return AjaxResult.error(error);
        }
        return AjaxResult.success("已为 " + fName + " 领取档位 " + tierKey);
    }

    @ApiName("重置玩家领取")
    @ApiPublic
    @PostMapping("/admin/tiers/{tier}/reset")
    public AjaxResult resetFor(ApiRequestContext ctx, @PathVariable(name = "tier") String tierKey,
                               @RequestBody String body) {
        AjaxResult deny = denyIfNotOp(ctx);
        if (deny != null) {
            return deny;
        }
        String targetName = str(JsonReader.parseObject(body).get("player"));
        if (targetName == null || targetName.isEmpty()) {
            return AjaxResult.error("请指定玩家名");
        }
        OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
        boolean removed = plugin.getStorage().resetTier(off.getUniqueId(), tierKey);
        if (!removed) {
            return AjaxResult.error(targetName + " 本就没有档位 " + tierKey + " 的领取记录");
        }
        return AjaxResult.success("已重置 " + targetName + " 的档位 " + tierKey + " 领取记录");
    }

    @ApiName("档位领取记录")
    @ApiPublic
    @GetMapping("/admin/tiers/{tier}/records")
    public AjaxResult tierRecords(ApiRequestContext ctx, @PathVariable(name = "tier") String tierKey) {
        AjaxResult deny = denyIfNotOp(ctx);
        if (deny != null) {
            return deny;
        }
        List<Map<String, Object>> records = new ArrayList<>();
        Set<UUID> uuids = plugin.getStorage().loadAllUuids();
        for (UUID uuid : uuids) {
            ClaimRecord rec = plugin.getStorage().loadClaims(uuid);
            if (rec == null || rec.getTier(tierKey) == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("uuid", uuid.toString());
            row.put("name", Bukkit.getOfflinePlayer(uuid).getName());
            row.put("month", rec.getTier(tierKey));
            records.add(row);
        }
        records.sort(Comparator.comparing(r -> str(r.get("month")), Comparator.nullsLast(Comparator.naturalOrder())));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tier", tierKey);
        data.put("total", records.size());
        data.put("records", records);
        return AjaxResult.success(data);
    }

    // ================================================================
    //  管理侧：设置
    // ================================================================

    @ApiName("读取设置")
    @ApiPublic
    @GetMapping("/admin/settings")
    public AjaxResult getSettings(ApiRequestContext ctx) {
        AjaxResult deny = denyIfNotOp(ctx);
        if (deny != null) {
            return deny;
        }
        FileConfiguration cfg = plugin.getConfig();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("settings", cfg.getConfigurationSection("settings").getValues(true));
        // 存储后端开关与镜像（不回传数据库密码等敏感连接信息）
        Map<String, Object> backends = new LinkedHashMap<>();
        ConfigurationSection bs = cfg.getConfigurationSection("storage.backends");
        if (bs != null) {
            for (String id : bs.getKeys(false)) {
                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("enabled", bs.getConfigurationSection(id).getBoolean("enabled", false));
                backends.put(id, meta);
            }
        }
        data.put("backends", backends);
        data.put("mirror", cfg.getConfigurationSection("storage.mirror").getValues(true));
        return AjaxResult.success(data);
    }

    @ApiName("保存设置")
    @ApiPublic
    @PutMapping("/admin/settings")
    public AjaxResult saveSettings(ApiRequestContext ctx, @RequestBody String body) {
        AjaxResult deny = denyIfNotOp(ctx);
        if (deny != null) {
            return deny;
        }
        Map<String, Object> payload = JsonReader.parseObject(body);
        try {
            // 基础设置白名单
            String[] allowed = {
                    "claim-day", "auto-claim-on-join", "month-format",
                    "offline-compensation", "debug", "audit.enabled"
            };
            Object settings = payload.get("settings");
            if (settings instanceof Map) {
                Map<?, ?> sm = (Map<?, ?>) settings;
                for (String key : allowed) {
                    if (sm.containsKey(key)) {
                        plugin.getConfig().set("settings." + key, sm.get(key));
                    }
                }
                if (sm.containsKey("units") && sm.get("units") instanceof Map) {
                    Map<?, ?> units = (Map<?, ?>) sm.get("units");
                    if (units.get("money") != null) {
                        plugin.getConfig().set("settings.units.money", str(units.get("money")));
                    }
                    if (units.get("points") != null) {
                        plugin.getConfig().set("settings.units.points", str(units.get("points")));
                    }
                }
            }
            // 存储后端开关
            Object backends = payload.get("backends");
            if (backends instanceof Map) {
                for (Map.Entry<?, ?> e : ((Map<?, ?>) backends).entrySet()) {
                    String id = str(e.getKey());
                    if (e.getValue() instanceof Map && ((Map<?, ?>) e.getValue()).containsKey("enabled")) {
                        Object en = ((Map<?, ?>) e.getValue()).get("enabled");
                        plugin.getConfig().set("storage.backends." + id + ".enabled", en);
                    }
                }
            }
            // 镜像设置
            Object mirror = payload.get("mirror");
            if (mirror instanceof Map) {
                Map<?, ?> mm = (Map<?, ?>) mirror;
                for (Map.Entry<?, ?> e : mm.entrySet()) {
                    plugin.getConfig().set("storage.mirror." + str(e.getKey()), e.getValue());
                }
            }
            plugin.saveConfig();
            plugin.getConfigManager().reload();
            plugin.reloadAuditLogger();
            return AjaxResult.success("设置已保存（存储后端变更需重启服务器生效）");
        } catch (Exception e) {
            return AjaxResult.error("保存设置失败: " + e.getMessage());
        }
    }

    // ================================================================
    //  管理侧：日志 / 背包 / 玩家
    // ================================================================

    @ApiName("读取审计日志")
    @ApiPublic
    @GetMapping("/admin/logs")
    public AjaxResult getLogs(ApiRequestContext ctx) {
        AjaxResult deny = denyIfNotOp(ctx);
        if (deny != null) {
            return deny;
        }
        String path = plugin.getConfig().getString("settings.audit.file", "logs/claims.log");
        File file = new File(plugin.getDataFolder(), path);
        List<String> lines = new ArrayList<>();
        if (file.exists()) {
            try {
                List<String> all = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                int from = Math.max(0, all.size() - 500);
                lines = all.subList(from, all.size());
            } catch (Exception e) {
                return AjaxResult.error("读取日志失败: " + e.getMessage());
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", plugin.getAuditLogger().isEnabled());
        data.put("file", path);
        data.put("lines", lines);
        return AjaxResult.success(data);
    }

    @ApiName("读取管理员背包")
    @ApiPublic
    @GetMapping("/admin/inventory")
    public AjaxResult getInventory(ApiRequestContext ctx) {
        AjaxResult deny = denyIfNotOp(ctx);
        if (deny != null) {
            return deny;
        }
        Player operator = ctx.getSyncPlayer();
        if (operator == null) {
            return AjaxResult.error("管理员不在线，无法读取背包");
        }
        // 背包读取切主线程（getSyncPlayer 已在主线程取过，这里内容读取同样需主线程）
        List<Map<String, Object>> contents = Sync.run(plugin,
                () -> ItemSerializer.serializeAll(operator.getInventory().getContents()));
        List<Map<String, Object>> hotbar = Sync.run(plugin,
                () -> ItemSerializer.serializeAll(operator.getInventory().getStorageContents()));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("operator", operator.getName());
        data.put("items", contents);
        data.put("storage", hotbar);
        return AjaxResult.success(data);
    }

    @ApiName("在线玩家列表")
    @ApiPublic
    @GetMapping("/admin/players")
    public AjaxResult onlinePlayers(ApiRequestContext ctx) {
        AjaxResult deny = denyIfNotOp(ctx);
        if (deny != null) {
            return deny;
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", p.getName());
            m.put("uuid", p.getUniqueId().toString());
            m.put("op", p.isOp());
            out.add(m);
        }
        return AjaxResult.success(out);
    }

    // ================================================================
    //  辅助方法
    // ================================================================

    /** 校验当前请求来自在线 OP；通过返回 null，否则返回错误响应。 */
    private AjaxResult denyIfNotOp(ApiRequestContext ctx) {
        String name = ctx == null ? null : ctx.getPlayerName();
        if (name == null || name.isEmpty()) {
            return AjaxResult.unauthorized("未认证或凭证无效，无法识别操作者");
        }
        Player online = ctx.getSyncPlayer();
        if (online == null) {
            return AjaxResult.forbidden("仅限在线 OP 操作：凭证玩家 " + name + " 当前不在服务器内");
        }
        if (!online.isOp()) {
            return AjaxResult.forbidden("无权限：仅 OP 可操作（当前: " + name + "）");
        }
        return null;
    }

    private List<String> tierKeys() {
        ConfigurationSection tiers = plugin.getRewards().getConfigurationSection("tiers");
        return tiers == null ? Collections.<String>emptyList() : new ArrayList<>(tiers.getKeys(false));
    }

    private ConfigurationSection tierSection(String tierKey) {
        return plugin.getRewards().getConfigurationSection("tiers." + tierKey);
    }

    /** 用户侧档位展示 DTO。 */
    private Map<String, Object> tierView(String key, ConfigurationSection tier) {
        ConfigurationSection rw = tier.getConfigurationSection("rewards");
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("key", key);
        dto.put("money", rw == null ? 0 : rw.getInt("money", 0));
        dto.put("points", rw == null ? 0 : rw.getInt("points", 0));
        dto.put("commands", rw == null ? Collections.emptyList() : rw.getStringList("commands"));
        dto.put("items", rw == null ? Collections.emptyList() : rw.getMapList("items"));
        return dto;
    }

    /** 管理侧档位完整详情。 */
    private Map<String, Object> tierDetail(String key, ConfigurationSection tier) {
        ConfigurationSection rw = tier.getConfigurationSection("rewards");
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("key", key);
        dto.put("permission", tier.getString("permission", ""));
        dto.put("enabled", tier.getBoolean("enabled", true));
        Map<String, Object> rewards = new LinkedHashMap<>();
        rewards.put("money", rw == null ? 0 : rw.getInt("money", 0));
        rewards.put("points", rw == null ? 0 : rw.getInt("points", 0));
        rewards.put("commands", rw == null ? Collections.emptyList() : rw.getStringList("commands"));
        rewards.put("items", rw == null ? Collections.emptyList() : rw.getMapList("items"));
        dto.put("rewards", rewards);
        return dto;
    }

    private YamlConfiguration rewardsYaml() {
        FileConfiguration cfg = plugin.getRewards();
        return cfg instanceof YamlConfiguration ? (YamlConfiguration) cfg : new YamlConfiguration();
    }

    /** 保存 rewards.yml 并重新加载，使领取逻辑立即使用新配置。 */
    private void saveRewards(YamlConfiguration rewards) throws Exception {
        rewards.save(new File(plugin.getDataFolder(), "rewards.yml"));
        plugin.reloadExternalFiles();
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static int toInt(Object o) {
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
    private static List<String> toStringList(Object o) {
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
