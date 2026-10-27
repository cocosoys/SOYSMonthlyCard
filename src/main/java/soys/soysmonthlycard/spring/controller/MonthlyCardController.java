package soys.soysmonthlycard.spring.controller;

import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPublic;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PathVariable;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PostMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PutMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestBody;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.web.ApiRequestContext;
import soys.soysmonthlycard.SOYSMonthlyCard;
import soys.soysmonthlycard.exception.WebException;
import soys.soysmonthlycard.spring.entity.vo.TierToggleVO;
import soys.soysmonthlycard.spring.service.MonthlyCardService;
import soys.soysmonthlycard.spring.impl.MonthlyCardServiceImpl;

/**
 * SOYSMonthlyCard 业务端点（瘦身控制器）。
 *
 * <p>本类只负责：从请求中取参 → 调用 {@link MonthlyCardService} → 用 {@link AjaxResult} 包装结果，
 * 并把 {@link WebException} 按类型映射为对应响应。所有业务逻辑、鉴权、文件读写、
 * 存储访问与主线程调度均在 {@code soys.soysmonthlycard.spring.service.impl} 中实现。</p>
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

    private final MonthlyCardService service;

    public MonthlyCardController(SOYSMonthlyCard plugin) {
        this.service = new MonthlyCardServiceImpl(plugin);
    }

    // ================================================================
    //  用户侧
    // ================================================================

    @ApiName("展示月卡档位")
    @ApiPublic
    @GetMapping("/view/tiers")
    public AjaxResult viewTiers() {
        return AjaxResult.success(service.viewTiers());
    }

    @ApiName("玩家领取状态")
    @ApiPublic
    @GetMapping("/player/state")
    public AjaxResult playerState(ApiRequestContext ctx) {
        return AjaxResult.success(service.playerState(ctx));
    }

    // ================================================================
    //  管理侧：概览 / 列表
    // ================================================================

    @ApiName("概览统计")
    @ApiPublic
    @GetMapping("/admin/overview")
    public AjaxResult overview(ApiRequestContext ctx) {
        try {
            return AjaxResult.success(service.overview(ctx));
        } catch (WebException e) {
            return toResponse(e);
        }
    }

    @ApiName("礼包档位列表")
    @ApiPublic
    @GetMapping("/admin/tiers")
    public AjaxResult listTiers(ApiRequestContext ctx) {
        try {
            return AjaxResult.success(service.listTiers(ctx));
        } catch (WebException e) {
            return toResponse(e);
        }
    }

    // ================================================================
    //  管理侧：档位详情 / 保存 / 启停
    // ================================================================

    @ApiName("档位完整配置")
    @ApiPublic
    @GetMapping("/admin/tiers/{tier}")
    public AjaxResult getTier(ApiRequestContext ctx, @PathVariable(name = "tier") String tierKey) {
        try {
            return AjaxResult.success(service.getTier(ctx, tierKey));
        } catch (WebException e) {
            return toResponse(e);
        }
    }

    @ApiName("保存档位配置")
    @ApiPublic
    @PutMapping("/admin/tiers/{tier}")
    public AjaxResult saveTier(ApiRequestContext ctx, @PathVariable(name = "tier") String tierKey,
                               @RequestBody String body) {
        try {
            return AjaxResult.success(service.saveTier(ctx, tierKey, body));
        } catch (WebException e) {
            return toResponse(e);
        }
    }

    @ApiName("启用/禁用档位")
    @ApiPublic
    @PostMapping("/admin/tiers/{tier}/toggle")
    public AjaxResult toggleTier(ApiRequestContext ctx, @PathVariable(name = "tier") String tierKey) {
        try {
            TierToggleVO vo = service.toggleTier(ctx, tierKey);
            String msg = (vo.isEnabled() ? "已启用档位 " : "已禁用档位 ") + tierKey;
            return AjaxResult.success(msg, vo);
        } catch (WebException e) {
            return toResponse(e);
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
        try {
            return AjaxResult.success(service.claimFor(ctx, tierKey, body));
        } catch (WebException e) {
            return toResponse(e);
        }
    }

    @ApiName("重置玩家领取")
    @ApiPublic
    @PostMapping("/admin/tiers/{tier}/reset")
    public AjaxResult resetFor(ApiRequestContext ctx, @PathVariable(name = "tier") String tierKey,
                               @RequestBody String body) {
        try {
            return AjaxResult.success(service.resetFor(ctx, tierKey, body));
        } catch (WebException e) {
            return toResponse(e);
        }
    }

    @ApiName("档位领取记录")
    @ApiPublic
    @GetMapping("/admin/tiers/{tier}/records")
    public AjaxResult tierRecords(ApiRequestContext ctx, @PathVariable(name = "tier") String tierKey) {
        try {
            return AjaxResult.success(service.tierRecords(ctx, tierKey));
        } catch (WebException e) {
            return toResponse(e);
        }
    }

    // ================================================================
    //  管理侧：设置
    // ================================================================

    @ApiName("读取设置")
    @ApiPublic
    @GetMapping("/admin/settings")
    public AjaxResult getSettings(ApiRequestContext ctx) {
        try {
            return AjaxResult.success(service.getSettings(ctx));
        } catch (WebException e) {
            return toResponse(e);
        }
    }

    @ApiName("保存设置")
    @ApiPublic
    @PutMapping("/admin/settings")
    public AjaxResult saveSettings(ApiRequestContext ctx, @RequestBody String body) {
        try {
            return AjaxResult.success(service.saveSettings(ctx, body));
        } catch (WebException e) {
            return toResponse(e);
        }
    }

    // ================================================================
    //  管理侧：日志 / 背包 / 玩家
    // ================================================================

    @ApiName("读取审计日志")
    @ApiPublic
    @GetMapping("/admin/logs")
    public AjaxResult getLogs(ApiRequestContext ctx) {
        try {
            return AjaxResult.success(service.getLogs(ctx));
        } catch (WebException e) {
            return toResponse(e);
        }
    }

    @ApiName("读取管理员背包")
    @ApiPublic
    @GetMapping("/admin/inventory")
    public AjaxResult getInventory(ApiRequestContext ctx) {
        try {
            return AjaxResult.success(service.getInventory(ctx));
        } catch (WebException e) {
            return toResponse(e);
        }
    }

    @ApiName("在线玩家列表")
    @ApiPublic
    @GetMapping("/admin/players")
    public AjaxResult onlinePlayers(ApiRequestContext ctx) {
        try {
            return AjaxResult.success(service.onlinePlayers(ctx));
        } catch (WebException e) {
            return toResponse(e);
        }
    }

    // ================================================================
    //  异常 → 响应映射（纯框架响应映射，非业务逻辑）
    // ================================================================

    private AjaxResult toResponse(WebException e) {
        switch (e.getKind()) {
            case UNAUTHORIZED:
                return AjaxResult.unauthorized(e.getMessage());
            case FORBIDDEN:
                return AjaxResult.forbidden(e.getMessage());
            case NOT_FOUND:
                return AjaxResult.notFound(e.getMessage());
            default:
                return AjaxResult.error(e.getMessage());
        }
    }
}
