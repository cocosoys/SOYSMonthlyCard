package soys.soysmonthlycard.erp.controller;

import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPermission;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PathVariable;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PostMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PutMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestBody;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestParam;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import soys.soysmonthlycard.SOYSMonthlyCard;
import soys.soysmonthlycard.erp.entity.vo.McTierSaveVO;
import soys.soysmonthlycard.erp.impl.McTierServiceImpl;
import soys.soysmonthlycard.erp.service.McTierService;

/**
 * 月卡档位管理（ERP 模块）：路由 {@code /api/plugins/SOYSMonthlyCard/soysmonthlycard/tier/*}。
 *
 * <p>控制器只做取参与委托，业务/文件读写全部在 {@link McTierServiceImpl}；
 * 列表返回框架分页契约 {@link TableDataInfo}，单体返回 {@link AjaxResult}。</p>
 */
@ApiName("月卡档位")
@RequestMapping("/soysmonthlycard/tier")
public class McTierController {

    private final McTierService service;

    public McTierController(SOYSMonthlyCard plugin) {
        this.service = new McTierServiceImpl(plugin);
    }

    @ApiName("档位列表")
    @ApiPermission("mcerp:monthlycard:tier:list")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(name = "pageNum", required = false) Integer pageNum,
                             @RequestParam(name = "pageSize", required = false) Integer pageSize,
                             @RequestParam(name = "keyword", required = false) String keyword) {
        return service.list(pageNum, pageSize, keyword);
    }

    @ApiName("档位详情")
    @ApiPermission("mcerp:monthlycard:tier:query")
    @GetMapping("/{tierKey}")
    public AjaxResult detail(@PathVariable(name = "tierKey") String tierKey) {
        return service.detail(tierKey);
    }

    @ApiName("保存档位")
    @ApiPermission("mcerp:monthlycard:tier:edit")
    @PutMapping("/{tierKey}")
    public AjaxResult save(@PathVariable(name = "tierKey") String tierKey,
                           @RequestBody McTierSaveVO body) {
        return service.save(tierKey, body);
    }

    @ApiName("启用/禁用档位")
    @ApiPermission("mcerp:monthlycard:tier:edit")
    @PostMapping("/{tierKey}/toggle")
    public AjaxResult toggle(@PathVariable(name = "tierKey") String tierKey) {
        return service.toggle(tierKey);
    }
}
