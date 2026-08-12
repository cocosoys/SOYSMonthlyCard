package soys.soysmonthlycard.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家月卡领取记录（存储层使用的领域对象）。
 * <p>tiers 保存「档位名 -&gt; 已领取月份(yyyy-MM)」的映射，例如 {"default":"2026-08","vip":"2026-08"}。</p>
 */
public class ClaimRecord {

    private final UUID uuid;
    private final Map<String, String> tiers;

    public ClaimRecord(UUID uuid) {
        this(uuid, new HashMap<>());
    }

    public ClaimRecord(UUID uuid, Map<String, String> tiers) {
        this.uuid = uuid;
        this.tiers = new HashMap<>(tiers);
    }

    public UUID getUuid() {
        return uuid;
    }

    /** 返回一份副本，避免外部修改内部状态 */
    public Map<String, String> getTiers() {
        return new HashMap<>(tiers);
    }

    /** 读取某档位已领取的月份；未领取返回 null */
    public String getTier(String tier) {
        return tiers.get(tier);
    }

    public void setTier(String tier, String month) {
        tiers.put(tier, month);
    }

    public void removeTier(String tier) {
        tiers.remove(tier);
    }

    public boolean isEmpty() {
        return tiers.isEmpty();
    }
}
