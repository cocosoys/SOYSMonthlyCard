package soys.soysmonthlycard.spring.entity.vo;

/** 档位领取记录行 VO（{@code GET /admin/tiers/{tier}/records} 的 records 元素）。 */
public class TierRecordVO {
    private String uuid;
    private String name;
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

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }
}
