package soys.soysmonthlycard.spring.entity.vo;

import java.util.List;
import java.util.Map;

/**
 * 管理员背包视图 VO（{@code GET /admin/inventory}）。
 *
 * <p>{@code items} / {@code storage} 为 {@code ItemSerializer} 输出的序列化物品 Map 列表。</p>
 */
public class InventoryVO {
    private String operator;
    private List<Map<String, Object>> items;
    private List<Map<String, Object>> storage;

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public List<Map<String, Object>> getItems() {
        return items;
    }

    public void setItems(List<Map<String, Object>> items) {
        this.items = items;
    }

    public List<Map<String, Object>> getStorage() {
        return storage;
    }

    public void setStorage(List<Map<String, Object>> storage) {
        this.storage = storage;
    }
}
