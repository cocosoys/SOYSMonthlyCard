package soys.soysmonthlycard.erp;

import com.github.cocosoys.mc.mcerp.ErpMenus;
import com.github.cocosoys.mc.mcerp.McerpExpansion;
import soys.soysmonthlycard.SOYSMonthlyCard;
import soys.soysmonthlycard.erp.controller.McClaimController;
import soys.soysmonthlycard.erp.controller.McStatController;
import soys.soysmonthlycard.erp.controller.McTierController;

import java.util.Arrays;
import java.util.List;

/**
 * SOYSMonthlyCard 的 MCERP 模块扩展（统一 ERP 控制台侧边栏入口）。
 *
 * <p>继承 {@link McerpExpansion} 后，{@code register()} 会自动完成两件事：</p>
 * <ul>
 *   <li>按 {@link #buildControllers()} 登记三个 ERP 控制器，挂
 *       {@code /api/plugins/SOYSMonthlyCard/soysmonthlycard/{tier,claim,stat}/*}；</li>
 *   <li>{@link #onRegister()} 默认实现把本模块（identifier/displayName/菜单树）写入
 *       MCERP {@code ErpRegistry}，菜单与路由即时合成。</li>
 * </ul>
 *
 * <p><b>前端页面归属</b>：三个菜单页是 <b>MCERP 统一控制台前端（MCERP-Vue2-example）里的
 * Vue 视图</b>，位于 {@code src/views/soysmonthlycarderp/{tier,claim,stat}/index.vue}，
 * 由控制台的 {@code store/modules/permission.js#loadView} 把
 * {@code wujie:/web/plugins/soysmonthlycard-erp/soysmonthlycarderp/xxx} 映射为本地组件。
 * 因此本扩展<b>不声明 {@code resourceRoot()}</b>：既不重复托管页面（页面登记以 owner 插件名为键，
 * 会与 {@code MonthlyCardExpansion} 争抢），也不需要自带 dist 子应用。</p>
 *
 * <p>MCERP 未安装时本类不会被加载（{@code SOYSMonthlyCard#setupErpExpansion} 先行探测），
 * 因此 MCERP 依赖为纯 softdepend。</p>
 */
public final class MonthlyCardErpExpansion extends McerpExpansion {

    private final SOYSMonthlyCard plugin;

    public MonthlyCardErpExpansion(SOYSMonthlyCard plugin) {
        this.plugin = plugin;
    }

    /** 模块 id：同时决定路由前缀 {@code /soysmonthlycarderp} 与页面目录名（NameSafe）。 */
    @Override
    public String getIdentifier() {
        return "soysmonthlycard-erp";
    }

    @Override
    public String getAuthor() {
        return "SOYS";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    /** ERP 侧边栏显示名。 */
    @Override
    protected String displayName() {
        return "月卡管理";
    }

    /** 侧边栏图标（ERP 前端图标标识）。 */
    @Override
    protected String icon() {
        return "gift";
    }

    /** 排在系统管理之后。 */
    @Override
    protected int sortOrder() {
        return 50;
    }

    /**
     * 菜单树：档位配置 / 领取记录 / 统计与日志（三者为模块顶层子菜单）。
     *
     * <p><b>不再额外套一层 {@code dir}</b>：{@code MenuRouteServiceImpl#moduleRoute} 已用
     * {@link #displayName()}（"月卡管理"）+ {@code icon()} 合成模块顶层节点，再加一层目录
     * 会让路由多出一级（{@code /soysmonthlycarderp/soysmonthlycard/tier}），与控制台前端
     * 兜底路由 {@code /soysmonthlycarderp/tier} 对不上。</p>
     *
     * <p>component 走 {@link ErpMenus#component(String)}，由 {@code pageDirRoot()} 自动补前缀
     * {@code /web/plugins/soysmonthlycard-erp/soysmonthlycarderp}，MCERP 后端再包成
     * {@code wujie:<url>} 下发；控制台前端 {@code store/modules/permission.js#loadView}
     * 按通用规则 {@code /web/plugins/<插件>/<安全目录名>/<子路径> → <安全目录名>/<子路径>}
     * 映射到本地 Vue 视图 {@code src/views/soysmonthlycarderp/tier/index.vue}。</p>
     *
     * <p>每个 menu 带对应 controller 类，{@code ControllerPermScanner} 会把 {@code @ApiPermission}
     * 端点自动投影为 F 按钮；菜单自身 perms 用"列表/查看"级权限，无权限则该菜单不下发。</p>
     */
    @Override
    protected ErpMenus menus() {
        return ErpMenus.create()
                .menu("tier", "档位配置", "gift", McTierController.class)
                .component("/tier/index").perms("mcerp:monthlycard:tier:list")
                .menu("claim", "领取记录", "form", McClaimController.class)
                .component("/claim/index").perms("mcerp:monthlycard:claim:list")
                .menu("stat", "统计与日志", "chart", McStatController.class)
                .component("/stat/index").perms("mcerp:monthlycard:stat:view");
    }

    /** 待登记端点：三个 ERP 控制器（本类不写端点）。 */
    @Override
    protected List<Object> buildControllers() {
        return Arrays.asList(
                new McTierController(plugin),
                new McClaimController(plugin),
                new McStatController(plugin));
    }
}
