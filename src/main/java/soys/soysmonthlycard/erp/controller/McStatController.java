package soys.soysmonthlycard.erp.controller;

import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPermission;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PathVariable;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestParam;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import soys.soysmonthlycard.SOYSMonthlyCard;
import soys.soysmonthlycard.erp.impl.McStatServiceImpl;
import soys.soysmonthlycard.erp.service.McStatService;

/**
 * 月卡统计与运维（ERP 模块）：路由 {@code /api/plugins/SOYSMonthlyCard/soysmonthlycard/stat/*}。
 */
@ApiName("月卡统计")
@RequestMapping("/soysmonthlycard/stat")
public class McStatController {

    private final McStatService service;

    public McStatController(SOYSMonthlyCard plugin) {
        this.service = new McStatServiceImpl(plugin);
    }

    @ApiName("概览统计")
    @ApiPermission("mcerp:monthlycard:stat:view")
    @GetMapping("/overview")
    public AjaxResult overview() {
        return service.overview();
    }

    @ApiName("领取审计日志")
    @ApiPermission("mcerp:monthlycard:stat:logs")
    @GetMapping("/logs")
    public AjaxResult logs(@RequestParam(name = "maxLines", required = false) Integer maxLines) {
        return service.logs(maxLines);
    }

    @ApiName("玩家档位状态")
    @ApiPermission("mcerp:monthlycard:stat:view")
    @GetMapping("/player/{name}")
    public AjaxResult playerState(@PathVariable(name = "name") String name) {
        return service.playerState(name);
    }
}
