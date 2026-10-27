package soys.soysmonthlycard.erp.controller;

import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPermission;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PathVariable;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PostMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestBody;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestParam;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import soys.soysmonthlycard.SOYSMonthlyCard;
import soys.soysmonthlycard.erp.entity.vo.McPlayerVO;
import soys.soysmonthlycard.erp.impl.McClaimServiceImpl;
import soys.soysmonthlycard.erp.service.McClaimService;

/**
 * 月卡领取记录与管理（ERP 模块）：路由 {@code /api/plugins/SOYSMonthlyCard/soysmonthlycard/claim/*}。
 *
 * <p>代领走主线程调度（{@code web.Sync}），失败原因原样回传；重置操作幂等（无记录即报错提示）。</p>
 */
@ApiName("月卡领取")
@RequestMapping("/soysmonthlycard/claim")
public class McClaimController {

    private final McClaimService service;

    public McClaimController(SOYSMonthlyCard plugin) {
        this.service = new McClaimServiceImpl(plugin);
    }

    @ApiName("领取记录列表")
    @ApiPermission("mcerp:monthlycard:claim:list")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(name = "pageNum", required = false) Integer pageNum,
                             @RequestParam(name = "pageSize", required = false) Integer pageSize,
                             @RequestParam(name = "tierKey", required = false) String tierKey,
                             @RequestParam(name = "player", required = false) String player) {
        return service.list(pageNum, pageSize, tierKey, player);
    }

    @ApiName("代玩家领取")
    @ApiPermission("mcerp:monthlycard:claim:grant")
    @PostMapping("/{tierKey}/claim")
    public AjaxResult claim(@PathVariable(name = "tierKey") String tierKey,
                            @RequestBody McPlayerVO body) {
        return service.claim(tierKey, body == null ? null : body.getPlayer());
    }

    @ApiName("重置玩家领取")
    @ApiPermission("mcerp:monthlycard:claim:reset")
    @PostMapping("/{tierKey}/reset")
    public AjaxResult reset(@PathVariable(name = "tierKey") String tierKey,
                            @RequestBody McPlayerVO body) {
        return service.reset(tierKey, body == null ? null : body.getPlayer());
    }

    @ApiName("本月领取排行榜")
    @ApiPermission("mcerp:monthlycard:claim:list")
    @GetMapping("/top")
    public AjaxResult top(@RequestParam(name = "limit", required = false) Integer limit) {
        return service.top(limit);
    }
}
