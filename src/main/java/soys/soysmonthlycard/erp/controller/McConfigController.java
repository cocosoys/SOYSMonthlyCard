package soys.soysmonthlycard.erp.controller;

import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPermission;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PostMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestBody;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestParam;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import soys.soysmonthlycard.SOYSMonthlyCard;
import soys.soysmonthlycard.erp.impl.McConfigServiceImpl;
import soys.soysmonthlycard.erp.service.McConfigService;

import java.util.Map;

/**
 * ERP 管理面 ——「插件配置」菜单的可视化配置编辑端点。
 *
 * <p>路径：{@code /api/plugins/SOYSMonthlyCard/erp/config/*}，与前端
 * {@code web/src/api/erp.js} 的 {@code listConfigFiles/loadConfigFile/saveConfigFile} 一一对应
 * （三函数及 {@code configPage} mixin 均为工程既有设施，无需改动）。</p>
 *
 * <p>注解来自 {@code com.github.cocosoys.mc.soyshttpovermc.annotations}，是平台自定义注解，
 * <b>不是 Spring 的</b>。{@code @ApiPermission} 节点同时被 {@code ControllerPermScanner}
 * 投影为 ERP 菜单下的 F 按钮。</p>
 *
 * <p>控制器只做取参与委托，文件读写全部在 {@link McConfigServiceImpl}。</p>
 */
@RequestMapping("/erp/config")
public class McConfigController {

    private final McConfigService service;

    public McConfigController(SOYSMonthlyCard plugin) {
        this.service = new McConfigServiceImpl(plugin);
    }

    @ApiName("配置文件清单")
    @ApiPermission("mcerp:monthlycard:config:list")
    @GetMapping("/files")
    public AjaxResult files() {
        return service.list();
    }

    @ApiName("配置文件读取")
    @ApiPermission("mcerp:monthlycard:config:query")
    @GetMapping("/load")
    public AjaxResult load(@RequestParam(name = "file", required = true) String file) {
        return service.load(file);
    }

    @ApiName("配置文件保存")
    @ApiPermission("mcerp:monthlycard:config:edit")
    @PostMapping("/save")
    public AjaxResult save(@RequestParam(name = "file", required = true) String file,
                           @RequestBody Map<String, Object> data) {
        return service.save(file, data);
    }
}
