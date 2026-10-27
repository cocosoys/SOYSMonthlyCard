package soys.soysmonthlycard.erp.entity.vo;

import java.util.List;
import java.util.Map;

/**
 * 档位保存请求体（扁平字段，便于框架 JSON 绑定；null 字段表示不修改）。
 */
public class McTierSaveVO {
    private String permission;
    private Boolean enabled;
    private Integer money;
    private Integer points;
    private List<String> commands;
    private List<Map<String, Object>> items;

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getMoney() {
        return money;
    }

    public void setMoney(Integer money) {
        this.money = money;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }

    public List<Map<String, Object>> getItems() {
        return items;
    }

    public void setItems(List<Map<String, Object>> items) {
        this.items = items;
    }
}
