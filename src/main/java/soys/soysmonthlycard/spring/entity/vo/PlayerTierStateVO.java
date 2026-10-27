package soys.soysmonthlycard.spring.entity.vo;

/** 玩家单档位领取状态 VO（{@code GET /player/state} 的 tiers 元素）。 */
public class PlayerTierStateVO {
    private boolean claimed;
    private boolean permitted;

    public boolean isClaimed() {
        return claimed;
    }

    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }

    public boolean isPermitted() {
        return permitted;
    }

    public void setPermitted(boolean permitted) {
        this.permitted = permitted;
    }
}
