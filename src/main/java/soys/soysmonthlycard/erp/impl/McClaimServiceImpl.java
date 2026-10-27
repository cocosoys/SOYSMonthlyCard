package soys.soysmonthlycard.erp.impl;

import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.PageUtils;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import soys.soysmonthlycard.SOYSMonthlyCard;
import soys.soysmonthlycard.erp.entity.vo.McClaimRowVO;
import soys.soysmonthlycard.erp.entity.vo.McTopRowVO;
import soys.soysmonthlycard.erp.service.McClaimService;
import soys.soysmonthlycard.erp.util.McRewards;
import soys.soysmonthlycard.storage.ClaimRecord;
import soys.soysmonthlycard.web.Sync;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * {@link McClaimService} 实现：直接复用既有 {@code StorageManager} / {@code ClaimManager}，
 * 不新增数据表、不复制领取判定逻辑。
 */
public class McClaimServiceImpl implements McClaimService {

    /** 排行榜默认条数。 */
    private static final int DEFAULT_TOP = 10;

    /** 排行榜最大条数（防止前端传入过大值拖慢主线程）。 */
    private static final int MAX_TOP = 100;

    private final SOYSMonthlyCard plugin;

    public McClaimServiceImpl(SOYSMonthlyCard plugin) {
        this.plugin = plugin;
    }

    @Override
    public TableDataInfo list(Integer pageNum, Integer pageSize, String tierKey, String player) {
        String tier = trimToNull(tierKey);
        String kw = lower(trimToNull(player));
        List<McClaimRowVO> rows = new ArrayList<>();
        for (UUID uuid : plugin.getStorage().loadAllUuids()) {
            ClaimRecord record = plugin.getStorage().loadClaims(uuid);
            if (record == null) {
                continue;
            }
            String name = nameOf(uuid);
            if (kw != null && (name == null || !name.toLowerCase(Locale.ROOT).contains(kw))) {
                continue;
            }
            for (Map.Entry<String, String> entry : record.getTiers().entrySet()) {
                if (tier != null && !tier.equals(entry.getKey())) {
                    continue;
                }
                McClaimRowVO row = new McClaimRowVO();
                row.setUuid(uuid.toString());
                row.setName(name);
                row.setTier(entry.getKey());
                row.setMonth(entry.getValue());
                rows.add(row);
            }
        }
        // 月份倒序（最近领取在前），月份相同按档位名稳定排序
        rows.sort(Comparator.comparing(McClaimRowVO::getMonth, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(McClaimRowVO::getTier, Comparator.nullsLast(Comparator.naturalOrder())));
        return PageUtils.page(rows, pageNum, pageSize);
    }

    @Override
    public AjaxResult claim(String tierKey, String player) {
        String tier = trimToNull(tierKey);
        String target = trimToNull(player);
        if (tier == null) {
            return AjaxResult.error("请指定档位");
        }
        if (target == null) {
            return AjaxResult.error("请指定玩家名");
        }
        if (McRewards.tier(plugin, tier) == null) {
            return AjaxResult.notFound("档位不存在: " + tier);
        }
        // 发放奖励涉及 Bukkit API（背包 / 经济 / 指令），切回主线程执行
        String error = Sync.run(plugin, () -> {
            Player online = Bukkit.getPlayerExact(target);
            if (online == null) {
                return "玩家不在线，无法代领: " + target;
            }
            return plugin.getClaimManager().claimSingle(online, tier);
        });
        if (error != null && !error.isEmpty()) {
            return AjaxResult.error(error);
        }
        return ok("已为 " + target + " 领取档位 " + tier);
    }

    @Override
    public AjaxResult reset(String tierKey, String player) {
        String tier = trimToNull(tierKey);
        String target = trimToNull(player);
        if (tier == null) {
            return AjaxResult.error("请指定档位");
        }
        if (target == null) {
            return AjaxResult.error("请指定玩家名");
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(target);
        boolean removed = plugin.getStorage().resetTier(offline.getUniqueId(), tier);
        if (!removed) {
            return AjaxResult.error(target + " 本就没有档位 " + tier + " 的领取记录");
        }
        return ok("已重置 " + target + " 的档位 " + tier + " 领取记录");
    }

    @Override
    public AjaxResult top(Integer limit) {
        int size = limit == null || limit <= 0 ? DEFAULT_TOP : Math.min(limit, MAX_TOP);
        Map<UUID, Integer> counts = new LinkedHashMap<>();
        for (Map.Entry<UUID, List<String>> entry : plugin.getStorage().getClaimsThisMonth().entrySet()) {
            int count = entry.getValue() == null ? 0 : entry.getValue().size();
            if (count > 0) {
                counts.put(entry.getKey(), count);
            }
        }
        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        List<McTopRowVO> rows = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : sorted) {
            if (rows.size() >= size) {
                break;
            }
            McTopRowVO row = new McTopRowVO();
            row.setRank(rank++);
            row.setUuid(entry.getKey().toString());
            row.setName(nameOf(entry.getKey()));
            row.setCount(entry.getValue());
            rows.add(row);
        }
        return AjaxResult.success(rows);
    }

    // ===== 内部工具 =====

    /** 按 UUID 取玩家名（离线可查，名字丢失时回退 UUID 前 8 位）。 */
    private String nameOf(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? uuid.toString().substring(0, 8) : name;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String lower(String s) {
        return s == null ? null : s.toLowerCase(Locale.ROOT);
    }

    /** 仅消息的成功响应（{@code msg} 携带提示，{@code data} 留空）。 */
    private static AjaxResult ok(String msg) {
        return new AjaxResult(AjaxResult.SUCCESS, msg);
    }
}
