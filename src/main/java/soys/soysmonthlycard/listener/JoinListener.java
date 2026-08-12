package soys.soysmonthlycard.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import soys.soysmonthlycard.SOYSMonthlyCard;
import soys.soysmonthlycard.manager.ClaimManager;

/**
 * 玩家上线时根据配置自动尝试领取月卡礼包。
 */
public class JoinListener implements Listener {

    private final SOYSMonthlyCard plugin;

    public JoinListener(SOYSMonthlyCard plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfigManager().isAutoClaimOnJoin()) {
            return;
        }
        ClaimManager claimManager = plugin.getClaimManager();
        ClaimManager.ClaimSummary summary = claimManager.tryClaim(event.getPlayer());
        // 上线自动领取：仅成功时提示，避免每次上线刷屏；auto=true 使用离线补发文案
        claimManager.handleResult(event.getPlayer(), summary, true, true);
    }
}
