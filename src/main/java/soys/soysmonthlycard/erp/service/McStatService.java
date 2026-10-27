package soys.soysmonthlycard.erp.service;

import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

/**
 * 月卡统计与运维服务（ERP 模块）：概览、审计日志、玩家领取状态。
 */
public interface McStatService {

    /** 概览统计。 */
    AjaxResult overview();

    /** 审计日志（最近 N 行）。 */
    AjaxResult logs(Integer maxLines);

    /** 指定玩家的各档位领取状态。 */
    AjaxResult playerState(String name);
}
