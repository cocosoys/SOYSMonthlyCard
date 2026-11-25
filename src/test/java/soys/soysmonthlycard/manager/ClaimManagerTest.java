package soys.soysmonthlycard.manager;

import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ClaimManager 纯逻辑单元测试（无需服务器）。
 * 覆盖：领取时间窗口、月份格式、下次可领天数、档位权限收集。
 */
class ClaimManagerTest {

    @Test
    void isClaimable_dayGreaterOrEqualClaimDay() {
        assertTrue(ClaimManager.isClaimable(1, 1));
        assertTrue(ClaimManager.isClaimable(5, 5));
        assertTrue(ClaimManager.isClaimable(5, 20));
        assertFalse(ClaimManager.isClaimable(5, 4));
        assertFalse(ClaimManager.isClaimable(28, 1));
    }

    @Test
    void formatMonth_customAndFallback() {
        LocalDate d = LocalDate.of(2026, 8, 3);
        assertEquals("2026-08", ClaimManager.formatMonth(d, "yyyy-MM"));
        assertEquals("2026/08", ClaimManager.formatMonth(d, "yyyy/MM"));
        assertEquals("2026-08-03", ClaimManager.formatMonth(d, "yyyy-MM-dd"));
        // 空格式与非法格式都回退到 yyyy-MM
        assertEquals("2026-08", ClaimManager.formatMonth(d, null));
        assertEquals("2026-08", ClaimManager.formatMonth(d, ""));
        assertEquals("2026-08", ClaimManager.formatMonth(d, "not-a-pattern:["));
    }

    @Test
    void daysUntilNextClaimable() {
        // 已过领取日：算到下个月 claimDay
        assertEquals(5, ClaimManager.daysUntilNextClaimable(5, 30, 30)); // (30-30)+5
        assertEquals(10, ClaimManager.daysUntilNextClaimable(5, 25, 30)); // (30-25)+5
        // 未到领取日：算到本月 claimDay
        assertEquals(3, ClaimManager.daysUntilNextClaimable(5, 2, 30));
        // 已在窗口内（含领取日当天）：按设计返回到「下个月 claimDay」的天数
        assertEquals(30, ClaimManager.daysUntilNextClaimable(5, 5, 30));
    }

    @Test
    void collectEligibleTiers_permissionEnabledFiltering() {
        MemoryConfiguration cfg = new MemoryConfiguration();
        cfg.set("free.permission", "");
        cfg.set("free.enabled", true);
        cfg.set("vip.permission", "soysmonthlycard.tier.vip");
        cfg.set("vip.enabled", true);
        cfg.set("mvp.permission", "soysmonthlycard.tier.mvp");
        cfg.set("mvp.enabled", false); // 禁用档位应被跳过

        Player p = mock(Player.class);
        when(p.hasPermission("soysmonthlycard.tier.vip")).thenReturn(true);
        when(p.hasPermission("soysmonthlycard.tier.mvp")).thenReturn(true);

        List<String> tiers = ClaimManager.collectEligibleTiers(cfg, p);
        assertTrue(tiers.contains("free"), "空权限=对所有人开放，应入选");
        assertTrue(tiers.contains("vip"), "有权限且启用，应入选");
        assertFalse(tiers.contains("mvp"), "虽有权但被禁用，应跳过");

        Player noVip = mock(Player.class); // 无任何档位权限
        List<String> tiers2 = ClaimManager.collectEligibleTiers(cfg, noVip);
        assertTrue(tiers2.contains("free"));
        assertFalse(tiers2.contains("vip"));
    }

    @Test
    void collectEligibleTiers_nullSectionReturnsEmpty() {
        Player p = mock(Player.class);
        assertTrue(ClaimManager.collectEligibleTiers(null, p).isEmpty());
    }
}
