package soys.soysmonthlycard.spring.entity.vo;

import java.util.List;

/** 档位领取记录 VO（{@code GET /admin/tiers/{tier}/records}）。 */
public class TierRecordsVO {
    private String tier;
    private int total;
    private List<TierRecordVO> records;

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<TierRecordVO> getRecords() {
        return records;
    }

    public void setRecords(List<TierRecordVO> records) {
        this.records = records;
    }
}
