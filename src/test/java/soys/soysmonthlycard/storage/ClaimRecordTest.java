package soys.soysmonthlycard.storage;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** ClaimRecord 领域对象测试：副本隔离、增删、空判定。 */
class ClaimRecordTest {

    @Test
    void setGetRemoveTier() {
        UUID uuid = UUID.randomUUID();
        ClaimRecord rec = new ClaimRecord(uuid);
        assertTrue(rec.isEmpty());
        assertNull(rec.getTier("vip"));

        rec.setTier("vip", "2026-08");
        assertFalse(rec.isEmpty());
        assertEquals("2026-08", rec.getTier("vip"));

        rec.setTier("vip", "2026-09"); // 覆盖更新
        assertEquals("2026-09", rec.getTier("vip"));

        rec.removeTier("vip");
        assertTrue(rec.isEmpty());
        assertNull(rec.getTier("vip"));
    }

    @Test
    void getTiersReturnsDefensiveCopy() {
        Map<String, String> src = new HashMap<>();
        src.put("default", "2026-08");
        ClaimRecord rec = new ClaimRecord(UUID.randomUUID(), src);

        Map<String, String> copy = rec.getTiers();
        copy.put("vip", "2099-01"); // 改副本不应影响内部
        assertNull(rec.getTier("vip"));

        src.put("mvp", "2026-08"); // 构造时的入参后续修改也不应影响
        assertNull(rec.getTier("mvp"));
    }

    @Test
    void preservesUuid() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, new ClaimRecord(uuid).getUuid());
    }
}
