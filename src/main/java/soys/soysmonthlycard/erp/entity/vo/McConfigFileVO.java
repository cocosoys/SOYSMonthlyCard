package soys.soysmonthlycard.erp.entity.vo;

/**
 * 「插件配置」配置文件清单返回行（由 {@code MonthlyCardConfigFile} 派生）。
 *
 * <p>字段通过反射 getter 序列化，必须保留 public getter。</p>
 */
public class McConfigFileVO {

    private String id;
    private String name;
    private String file;
    private String desc;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
