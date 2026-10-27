package soys.soysmonthlycard.spring.entity.vo;

/** 管理侧礼包档位列表行 VO（{@code GET /admin/tiers} 的数组元素）。 */
public class TierListRowVO {
    private String key;
    private String permission;
    private boolean enabled;
    private int money;
    private int points;
    private int commandsCount;
    private int itemsCount;
    private int claimedThisMonth;

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

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getCommandsCount() {
        return commandsCount;
    }

    public void setCommandsCount(int commandsCount) {
        this.commandsCount = commandsCount;
    }

    public int getItemsCount() {
        return itemsCount;
    }

    public void setItemsCount(int itemsCount) {
        this.itemsCount = itemsCount;
    }

    public int getClaimedThisMonth() {
        return claimedThisMonth;
    }

    public void setClaimedThisMonth(int claimedThisMonth) {
        this.claimedThisMonth = claimedThisMonth;
    }
}
