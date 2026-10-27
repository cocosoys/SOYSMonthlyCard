package soys.soysmonthlycard.spring.entity.vo;

/** 在线玩家 VO（{@code GET /admin/players} 的数组元素）。 */
public class PlayerListVO {
    private String name;
    private String uuid;
    private boolean op;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isOp() {
        return op;
    }

    public void setOp(boolean op) {
        this.op = op;
    }
}
