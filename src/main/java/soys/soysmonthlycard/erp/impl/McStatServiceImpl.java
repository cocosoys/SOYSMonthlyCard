package soys.soysmonthlycard.erp.impl;

import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import soys.soysmonthlycard.SOYSMonthlyCard;
import soys.soysmonthlycard.erp.service.McStatService;
import soys.soysmonthlycard.erp.util.McRewards;
import soys.soysmonthlycard.spring.entity.vo.LogViewVO;
import soys.soysmonthlycard.spring.entity.vo.OverviewVO;
import soys.soysmonthlycard.spring.entity.vo.PlayerStateVO;
import soys.soysmonthlycard.spring.entity.vo.PlayerTierStateVO;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@link McStatService} 实现：概览 / 审计日志 / 玩家领取状态。
 *
 * <p>复用既有 {@code spring.entity.vo} 视图对象（{@link OverviewVO}/{@link LogViewVO}/
 * {@link PlayerStateVO}），保证 ERP 与用户后台返回结构一致，避免重复定义 VO。</p>
 */
public class McStatServiceImpl implements McStatService {

    /** 日志默认返回行数。 */
    private static final int DEFAULT_LOG_LINES = 200;

    /** 日志最多返回行数。 */
    private static final int MAX_LOG_LINES = 2000;

    private final SOYSMonthlyCard plugin;

    public McStatServiceImpl(SOYSMonthlyCard plugin) {
        this.plugin = plugin;
    }

    @Override
    public AjaxResult overview() {
        Map<UUID, List<String>> claimedMonth = plugin.getStorage().getClaimsThisMonth();
        int totalClaims = 0;
        for (List<String> tiers : claimedMonth.values()) {
            totalClaims += tiers == null ? 0 : tiers.size();
        }
        ConfigurationSection tiers = McRewards.tiers(plugin);
        OverviewVO vo = new OverviewVO();
        vo.setOnlinePlayers(Bukkit.getOnlinePlayers().size());
        vo.setRecordedPlayers(plugin.getStorage().countRecords());
        vo.setTiersCount(tiers == null ? 0 : tiers.getKeys(false).size());
        vo.setClaimedThisMonth(totalClaims);
        vo.setCurrentMonth(plugin.getClaimManager().currentMonth());
        vo.setClaimTime(plugin.getClaimManager().isClaimTime());
        vo.setPrimaryStorage(plugin.getStorage().getPrimary().getType().getDisplayName());
        vo.setAuditEnabled(plugin.getAuditLogger().isEnabled());
        return AjaxResult.success(vo);
    }

    @Override
    public AjaxResult logs(Integer maxLines) {
        int size = maxLines == null || maxLines <= 0 ? DEFAULT_LOG_LINES : Math.min(maxLines, MAX_LOG_LINES);
        String path = plugin.getConfig().getString("settings.audit.file", "logs/claims.log");
        File file = new File(plugin.getDataFolder(), path);
        List<String> lines = Collections.emptyList();
        if (file.exists()) {
            try {
                List<String> all = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                int from = Math.max(0, all.size() - size);
                lines = all.subList(from, all.size());
            } catch (Exception e) {
                return AjaxResult.error("读取日志失败: " + e.getMessage());
            }
        }
        LogViewVO vo = new LogViewVO();
        vo.setEnabled(plugin.getAuditLogger().isEnabled());
        vo.setFile(path);
        vo.setLines(lines);
        return AjaxResult.success(vo);
    }

    @Override
    public AjaxResult playerState(String name) {
        String target = name == null ? null : name.trim();
        if (target == null || target.isEmpty()) {
            return AjaxResult.error("请指定玩家名");
        }
        // 在线优先（权限判定直接走实体）；离线回退 OfflinePlayer + Vault 权限服务
        Player online = Bukkit.getPlayerExact(target);
        OfflinePlayer offline = online != null ? online : Bukkit.getOfflinePlayer(target);
        UUID uuid = offline.getUniqueId();
        Permission vault = online == null ? permissionProvider() : null;
        PlayerStateVO vo = new PlayerStateVO();
        vo.setName(online != null ? online.getName() : offline.getName());
        Map<String, PlayerTierStateVO> states = new LinkedHashMap<>();
        ConfigurationSection tiers = McRewards.tiers(plugin);
        if (tiers != null) {
            for (String key : tiers.getKeys(false)) {
                ConfigurationSection tier = tiers.getConfigurationSection(key);
                String perm = tier == null ? "" : tier.getString("permission", "");
                PlayerTierStateVO state = new PlayerTierStateVO();
                state.setClaimed(plugin.getClaimManager().hasClaimed(uuid, key));
                state.setPermitted(perm.isEmpty() || hasPermission(online, offline, vault, perm));
                states.put(key, state);
            }
        }
        vo.setTiers(states);
        return AjaxResult.success(vo);
    }

    // ===== 内部工具 =====

    /**
     * 权限判定：在线玩家直接查实体；离线玩家经 Vault 权限服务（LuckPerms/GroupManager 等）按名查询。
     *
     * <p>1.12.2 的 {@link OfflinePlayer} 未实现 {@code Permissible}，无法直接 {@code hasPermission}，
     * 故离线场景走 Vault；未安装权限插件时按无权限处理（宁可少展示，不误报可领）。</p>
     */
    private static boolean hasPermission(Player online, OfflinePlayer offline, Permission vault, String perm) {
        if (online != null) {
            return online.hasPermission(perm);
        }
        if (vault != null) {
            try {
                return vault.playerHas((String) null, offline.getName(), perm);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /** Vault 权限服务（未安装权限插件时返回 null）。 */
    private Permission permissionProvider() {
        try {
            RegisteredServiceProvider<Permission> rsp =
                    plugin.getServer().getServicesManager().getRegistration(Permission.class);
            return rsp == null ? null : rsp.getProvider();
        } catch (Throwable t) {
            return null;
        }
    }
}
