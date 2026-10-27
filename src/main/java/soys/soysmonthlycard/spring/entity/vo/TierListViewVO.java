package soys.soysmonthlycard.spring.entity.vo;

import java.util.List;

/** 用户侧月卡档位展示 VO（{@code GET /view/tiers}）。 */
public class TierListViewVO {
    private String month;
    private int claimDay;
    private boolean claimTime;
    private List<TierViewVO> tiers;

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public int getClaimDay() {
        return claimDay;
    }

    public void setClaimDay(int claimDay) {
        this.claimDay = claimDay;
    }

    public boolean isClaimTime() {
        return claimTime;
    }

    public void setClaimTime(boolean claimTime) {
        this.claimTime = claimTime;
    }

    public List<TierViewVO> getTiers() {
        return tiers;
    }

    public void setTiers(List<TierViewVO> tiers) {
        this.tiers = tiers;
    }
}
