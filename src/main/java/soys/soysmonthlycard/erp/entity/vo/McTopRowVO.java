package soys.soysmonthlycard.erp.entity.vo;

/** 本月领取排行榜行（{@code GET /soysmonthlycard/claim/top}）。 */
public class McTopRowVO {
    private int rank;
    private String uuid;
    private String name;
    private int count;

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

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

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
