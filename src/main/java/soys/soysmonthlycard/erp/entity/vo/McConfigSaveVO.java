package soys.soysmonthlycard.erp.entity.vo;

/**
 * 配置文件保存结果：文件名 + 热重载提示。
 *
 * <p>字段名 {@code tip} 必须与前端 {@code configPage} mixin 消费的 {@code res.data.tip} 对齐，
 * 否则保存后的提示弹窗不会出现。</p>
 */
public class McConfigSaveVO {

    private String file;
    private String tip;

    public static McConfigSaveVO of(String file, String tip) {
        McConfigSaveVO vo = new McConfigSaveVO();
        vo.file = file;
        vo.tip = tip;
        return vo;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getTip() {
        return tip;
    }

    public void setTip(String tip) {
        this.tip = tip;
    }
}
