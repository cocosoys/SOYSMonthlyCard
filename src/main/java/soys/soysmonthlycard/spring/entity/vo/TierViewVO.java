package soys.soysmonthlycard.spring.entity.vo;

import java.util.List;
import java.util.Map;

/** 用户侧月卡档位展示 VO（{@code GET /view/tiers} 的 tiers 元素）。 */
public class TierViewVO {
    private String key;
    private int money;
    private int points;
    private List<String> commands;
    private List<Map<String, Object>> items;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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
