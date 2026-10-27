package soys.soysmonthlycard.spring.entity.vo;

/** 管理侧档位完整配置 VO（{@code GET /admin/tiers/{tier}}）。 */
public class TierDetailVO {
    private String key;
    private String permission;
    private boolean enabled;
    private RewardsVO rewards;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public RewardsVO getRewards() {
        return rewards;
    }

    public void setRewards(RewardsVO rewards) {
        this.rewards = rewards;
    }
}
