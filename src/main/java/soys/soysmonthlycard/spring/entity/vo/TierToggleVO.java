package soys.soysmonthlycard.spring.entity.vo;

/** 档位启停结果 VO（{@code POST /admin/tiers/{tier}/toggle} 的 data）。 */
public class TierToggleVO {
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
