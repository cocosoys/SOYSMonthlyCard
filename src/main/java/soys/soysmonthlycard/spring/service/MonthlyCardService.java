package soys.soysmonthlycard.spring.service;

import com.github.cocosoys.mc.soyshttpovermc.web.ApiRequestContext;
import soys.soysmonthlycard.exception.WebException;
import soys.soysmonthlycard.spring.entity.vo.InventoryVO;
import soys.soysmonthlycard.spring.entity.vo.LogViewVO;
import soys.soysmonthlycard.spring.entity.vo.OverviewVO;
import soys.soysmonthlycard.spring.entity.vo.PlayerListVO;
import soys.soysmonthlycard.spring.entity.vo.PlayerStateVO;
import soys.soysmonthlycard.spring.entity.vo.SettingsViewVO;
import soys.soysmonthlycard.spring.entity.vo.TierDetailVO;
import soys.soysmonthlycard.spring.entity.vo.TierListViewVO;
import soys.soysmonthlycard.spring.entity.vo.TierListRowVO;
import soys.soysmonthlycard.spring.entity.vo.TierRecordsVO;
import soys.soysmonthlycard.spring.entity.vo.TierToggleVO;

import java.util.List;

/**
 * SOYSMonthlyCard 业务服务抽象层。
 *
 * <p>控制器（{@code web.controller}）只做"取参 → 调服务 → 包装 AjaxResult"，
 * 具体业务逻辑、鉴权、文件读写、存储访问、主线程调度全部由 {@code service.impl} 实现，
 * 并由 impl 负责相关"事务"（文件写 + reload、主线程调度）。</p>
 *
 * <p>失败一律通过 {@link WebException} 抛出，
 * 由控制器按异常类型映射为对应响应。</p>
 */
public interface MonthlyCardService {

    // ---------- 用户侧 ----------

    /** 展示用月卡档位（仅启用，含物品/金币/点券）。 */
    TierListViewVO viewTiers();

    /** 当前玩家各档位本月领取状态。 */
    PlayerStateVO playerState(ApiRequestContext ctx);

    // ---------- 管理侧：概览 / 列表 ----------

    /** 概览统计。 */
    OverviewVO overview(ApiRequestContext ctx);

    /** 礼包档位列表。 */
    List<TierListRowVO> listTiers(ApiRequestContext ctx);

    // ---------- 管理侧：档位详情 / 保存 / 启停 ----------

    /** 档位完整配置（含物品）。 */
    TierDetailVO getTier(ApiRequestContext ctx, String tierKey);

    /** 保存档位配置与物品。 */
    String saveTier(ApiRequestContext ctx, String tierKey, String body);

    /** 启用/禁用档位。 */
    TierToggleVO toggleTier(ApiRequestContext ctx, String tierKey);

    // ---------- 管理侧：代领 / 重置 / 记录 ----------

    /** 代在线玩家领取。 */
    String claimFor(ApiRequestContext ctx, String tierKey, String body);

    /** 重置玩家领取记录。 */
    String resetFor(ApiRequestContext ctx, String tierKey, String body);

    /** 该档位领取记录。 */
    TierRecordsVO tierRecords(ApiRequestContext ctx, String tierKey);

    // ---------- 管理侧：设置 ----------

    /** 读取设置。 */
    SettingsViewVO getSettings(ApiRequestContext ctx);

    /** 保存设置。 */
    String saveSettings(ApiRequestContext ctx, String body);

    // ---------- 管理侧：日志 / 背包 / 玩家 ----------

    /** 审计日志。 */
    LogViewVO getLogs(ApiRequestContext ctx);

    /** 当前管理员背包（拷贝物品用）。 */
    InventoryVO getInventory(ApiRequestContext ctx);

    /** 在线玩家列表。 */
    List<PlayerListVO> onlinePlayers(ApiRequestContext ctx);
}
