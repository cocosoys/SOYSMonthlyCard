package soys.soysmonthlycard.spring.entity.vo;

import java.util.Map;

/** 玩家领取状态 VO（{@code GET /player/state}）。 */
public class PlayerStateVO {
    private String name;
    private Map<String, PlayerTierStateVO> tiers;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, PlayerTierStateVO> getTiers() {
        return tiers;
    }

    public void setTiers(Map<String, PlayerTierStateVO> tiers) {
        this.tiers = tiers;
    }
}
