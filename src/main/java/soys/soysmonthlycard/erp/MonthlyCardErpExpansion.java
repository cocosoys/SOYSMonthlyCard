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
 * <p><b>前端页面归属</b>：三个菜单页全部落在 <b>本插件自己的 web 子应用</b>
 * （{@code web/src/views/monthlycard/{tiers,claims,logs}/index.vue}，随 jar 内 dist 托管在
 * {@code /web/plugins/SOYSMonthlyCard/}，页面登记挂在 owner 插件名下）。因此本扩展<b>不声明
 * {@code resourceRoot()}</b>：页面登记以 owner 插件名为键，重复登记会与
 * {@code MonthlyCardExpansion} 争抢同一前缀。</p>
 *
 * <p><b>component 为什么必须写全路径</b>：{@code McerpExpansion.toModuleVO()} 是
 * 「先 {@code menus()}、后 {@code setBasePrefix(pageDirRoot())}」，即 DSL 里调用
 * {@code .component(x)} 时 {@code basePrefix} 仍是空串，拼出的 component 就是裸 {@code x}；
 * 而 {@code build()} 的 {@code fillComponentIfAbsent} 只补空值，不会再修正非空 component。
 * 所以这里用 {@link ErpMenus#componentFullURL(String)} 直接写「宿主前缀 + 子应用路由」的完整路径，
 * 不走自动前缀。裸路径会导致 MCERP 控制台的 {@code WujieLink} 正则
 * {@code /\/web\/plugins\/([^/]+)/} 匹配失败、退回兜底名 {@code erp-subapp}，
 * 随后去站点根目录取页面 → 404 → 内容区白屏。</p>
 *
 * <p>MCERP 未安装时本类不会被加载（{@code SOYSMonthlyCard#setupErpExpansion} 先行探测），
 * 因此 MCERP 依赖为纯 softdepend。</p>
 */
public final class MonthlyCardErpExpansion extends McerpExpansion {

    /**
     * 子应用页面根：owner 插件 web 前缀 + 模块路由根。
     *
     * <p>第二段必须与 {@link #getIdentifier()} 经 MCERP {@code NameSafe.path()} 派生出的模块路由
     * 根一致（{@code soysmonthlycard-erp} → {@code soysmonthlycarderp}）。子应用
     * {@code web/src/router/index.js} 里的静态路由就注册在该根下（tier / claim / stat），
     * 主应用 {@code WujieLink} 取到子路径后通过 bus 推给子应用，两边必须逐字相同。</p>
     */
    private static final String PAGE_ROOT = "/web/plugins/SOYSMonthlyCard/soysmonthlycarderp";

    private final SOYSMonthlyCard plugin;

    public MonthlyCardErpExpansion(SOYSMonthlyCard plugin) {
        this.plugin = plugin;
    }

    /** 模块 id：同时决定路由前缀 {@code /soysmonthlycarderp} 与页面目录名（NameSafe）。 */
    @Override
    public String getIdentifier() {
        return "SOYSMonthlyCard-ERP";
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
     * 会让路由多出一级 {@code /soysmonthlycarderp/soysmonthlycard/tier}。</p>
     *
     * <p>component 用 {@link ErpMenus#componentFullURL(String)}，写「宿主前缀 + 子应用路由」全路径：
     * {@code /web/plugins/SOYSMonthlyCard/soysmonthlycarderp/{tier,claim,stat}}。原因见类注释——
     * DSL 执行时 {@code basePrefix} 尚未注入，相对路径会被拼成裸路径并永久失效。</p>
     *
     * <p>三段与 {@code web/src/router/index.js} 的 routeBindings 一一对应
     * （menuId → path → 静态路由 → 视图组件）；{@code WujieLink} 会取插件名
     * {@code SOYSMonthlyCard}、子路径 {@code /soysmonthlycarderp/xxx}，通过全局 bus 推给子应用路由。
     * <b>改这里必须同步改前端绑定表</b>，否则菜单树裁剪时查不到组件会直接抛错。</p>
     *
     * <p>每个 menu 带对应 controller 类，{@code ControllerPermScanner} 会把 {@code @ApiPermission}
     * 端点自动投影为 F 按钮；菜单自身 perms 用"列表/查看"级权限，无权限则该菜单不下发。</p>
     */
    @Override
    protected ErpMenus menus() {
        return ErpMenus.create()
                .menu("tier", "档位配置", "gift", McTierController.class)
                .componentFullURL(PAGE_ROOT + "/tier").perms("mcerp:monthlycard:tier:list")
                .menu("claim", "领取记录", "form", McClaimController.class)
                .componentFullURL(PAGE_ROOT + "/claim").perms("mcerp:monthlycard:claim:list")
                .menu("stat", "统计与日志", "chart", McStatController.class)
                .componentFullURL(PAGE_ROOT + "/stat").perms("mcerp:monthlycard:stat:view");
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
