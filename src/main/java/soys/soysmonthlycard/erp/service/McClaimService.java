package soys.soysmonthlycard.erp.service;

import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;

/**
 * 月卡领取记录服务（ERP 模块）。
 */
public interface McClaimService {

    /** 领取记录列表（可按档位 / 玩家过滤，内存分页）。 */
    TableDataInfo list(Integer pageNum, Integer pageSize, String tierKey, String player);

    /** 代在线玩家领取指定档位。 */
    AjaxResult claim(String tierKey, String player);

    /** 重置玩家某档位的领取记录。 */
    AjaxResult reset(String tierKey, String player);

    /** 本月已领取排行榜（前 N 名文本行）。 */
    AjaxResult top(Integer limit);
}
