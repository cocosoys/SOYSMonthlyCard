package soys.soysmonthlycard.erp.entity.vo;

/** 领取记录行（TableDataInfo.rows 元素）。 */
public class McClaimRowVO {
    private String uuid;
    private String name;
    private String tier;
    private String month;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }
}
