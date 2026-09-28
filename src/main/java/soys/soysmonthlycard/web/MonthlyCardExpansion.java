package soys.soysmonthlycard.web;

import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.api.SoysExpansion;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import soys.soysmonthlycard.SOYSMonthlyCard;

import java.util.Arrays;
import java.util.List;

/**
 * SOYSMonthlyCard 的 SOYSHTTPOverMC 扩展定义。
 *
 * <p>由主类在检测到 SOYSHTTPOverMC 已加载时创建并 {@link #register()}：
 * 框架自动完成业务端点注册、{@code dist/} 页面托管（用户侧 + 管理 ERP）、CORS 与卸载。</p>
 *
 * <p>端点统一挂 {@code /api/plugins/soysmonthlycard} 前缀；
 * 页面托管于 {@code /web/plugins/soysmonthlycard}。</p>
 */
public final class MonthlyCardExpansion extends SoysExpansion {

    private final SOYSMonthlyCard plugin;
    private final MonthlyCardController controller;

    public MonthlyCardExpansion(SOYSMonthlyCard plugin) {
        this.plugin = plugin;
        this.controller = new MonthlyCardController(plugin);
    }

    @Override
    public String getIdentifier() {
        return "soysmonthlycard";
    }

    @Override
    public String getAuthor() {
        return "SOYS";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    /** 前端资源根：自动托管 jar 内 dist/（磁盘 plugins/SOYSMonthlyCard/dist 优先，支持热替换）。 */
    @Override
    protected String resourceRoot() {
        return "dist";
    }

    /** 待注册端点：本扩展健康检查 + 业务控制器。 */
    @Override
    protected List<Object> buildControllers() {
        return Arrays.asList(this, controller);
    }

    /** 放开 CORS，便于网页在同源之外调用（管理面板/外部面板）。 */
    @Override
    protected CorsSpec[] cors() {
        return new CorsSpec[]{
                new CorsSpec("/api/plugins/soysmonthlycard", "*",
                        "GET,POST,PUT,DELETE,OPTIONS", "*", false)
        };
    }

    @ApiName("月卡插件健康检查")
    @GetMapping("/ping")
    public AjaxResult ping() {
        return AjaxResult.success("pong");
    }
}
