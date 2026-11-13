package soys.soysmonthlycard.erp.entity;

/**
 * SOYSMonthlyCard ERP「插件配置」菜单可编辑的配置文件清单 —— 数据驱动的唯一事实源。
 *
 * <p>菜单、HTTP 端点、文件读写全部从本枚举派生：新增一个可编辑文件 = 枚举加一项 + 加一个前端 .vue，
 * 其余链路自动打通。数据目录 = owner 插件自己的 {@code plugins/SOYSMonthlyCard/}。</p>
 *
 * <p><b>范围</b>：当前仅开放 {@code config.yml}。
 * {@code rewards.yml} 由「档位配置」页（{@code McTierController}）专门维护、
 * {@code language/*.yml} 由语言包机制维护，均不纳入，避免同一文件多入口写入互相覆盖。</p>
 *
 * <p><b>生效方式</b>：保存后执行 {@code /monthlycard reload} 热重载；
 * 存储后端 / 连接串等变更仍需重启服务器。注意 Bukkit {@code saveDefaultConfig()} 只在文件不存在时写入，
 * 旧配置不会自动合并新增节，故前端配置页必须做结构兜底（见 config/index.vue 的 ensureSkeleton）。</p>
 */
public enum MonthlyCardConfigFile {

    CONFIG("config", "核心配置", "config.yml",
            "领取日、自动领取、审计、语言、单位文案、存储后端与镜像策略");

    private final String id;
    private final String name;
    private final String file;
    private final String desc;

    MonthlyCardConfigFile(String id, String name, String file, String desc) {
        this.id = id;
        this.name = name;
        this.file = file;
        this.desc = desc;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFile() {
        return file;
    }

    public String getDesc() {
        return desc;
    }

    public static MonthlyCardConfigFile byId(String id) {
        if (id == null) {
            return null;
        }
        for (MonthlyCardConfigFile f : values()) {
            if (f.id.equals(id)) {
                return f;
            }
        }
        return null;
    }
}
