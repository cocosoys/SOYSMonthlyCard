package soys.soysmonthlycard.erp.service;

import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import soys.soysmonthlycard.erp.entity.vo.McTierSaveVO;

/**
 * 月卡档位管理服务（ERP 模块）。
 *
 * <p>返回体统一使用 SOYSHTTPOverMC 提供的能力：列表 → {@link TableDataInfo}（RuoYi 分页契约），
 * 单体 → {@link AjaxResult}。</p>
 */
public interface McTierService {

    /** 档位列表（内存筛选 + {@code PageUtils} 分页）。 */
    TableDataInfo list(Integer pageNum, Integer pageSize, String keyword);

    /** 档位完整配置（含物品）。 */
    AjaxResult detail(String tierKey);

    /** 保存档位配置（null 字段不修改）。 */
    AjaxResult save(String tierKey, McTierSaveVO body);

    /** 启用/禁用档位。 */
    AjaxResult toggle(String tierKey);
}
