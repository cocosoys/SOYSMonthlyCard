package soys.soysmonthlycard.spring.entity.vo;

/** 概览统计 VO（{@code GET /admin/overview}）。 */
public class OverviewVO {
    private int onlinePlayers;
    private int recordedPlayers;
    private int tiersCount;
    private int claimedThisMonth;
    private String currentMonth;
    private boolean claimTime;
    private String primaryStorage;
    private boolean auditEnabled;

    public int getOnlinePlayers() {
        return onlinePlayers;
    }

    public void setOnlinePlayers(int onlinePlayers) {
        this.onlinePlayers = onlinePlayers;
    }

    public int getRecordedPlayers() {
        return recordedPlayers;
    }

    public void setRecordedPlayers(int recordedPlayers) {
        this.recordedPlayers = recordedPlayers;
    }

    public int getTiersCount() {
        return tiersCount;
    }

    public void setTiersCount(int tiersCount) {
        this.tiersCount = tiersCount;
    }

    public int getClaimedThisMonth() {
        return claimedThisMonth;
    }

    public void setClaimedThisMonth(int claimedThisMonth) {
        this.claimedThisMonth = claimedThisMonth;
    }

    public String getCurrentMonth() {
        return currentMonth;
    }

    public void setCurrentMonth(String currentMonth) {
        this.currentMonth = currentMonth;
    }

    public boolean isClaimTime() {
        return claimTime;
    }

    public void setClaimTime(boolean claimTime) {
        this.claimTime = claimTime;
    }

    public String getPrimaryStorage() {
        return primaryStorage;
    }

    public void setPrimaryStorage(String primaryStorage) {
        this.primaryStorage = primaryStorage;
    }

    public boolean isAuditEnabled() {
        return auditEnabled;
    }

    public void setAuditEnabled(boolean auditEnabled) {
        this.auditEnabled = auditEnabled;
    }
}
