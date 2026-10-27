package soys.soysmonthlycard.spring.entity.vo;

import java.util.List;
import java.util.Map;

/** 档位奖励内容 VO（{@code TierDetailVO.rewards}）。 */
public class RewardsVO {
    private int money;
    private int points;
    private List<String> commands;
    private List<Map<String, Object>> items;

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
