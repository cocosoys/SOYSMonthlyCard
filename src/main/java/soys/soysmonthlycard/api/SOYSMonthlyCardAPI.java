package soys.soysmonthlycard.api;

import org.bukkit.entity.Player;
import soys.soysmonthlycard.SOYSMonthlyCard;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/**
 * 对外 API（通过 {@link SOYSMonthlyCard#getApi()} 获取）。
 * 其他插件可注册领取前钩子，在发放奖励前拦截或放行。
 *
 * <pre>
 * SOYSMonthlyCard plugin = (SOYSMonthlyCard) Bukkit.getPluginManager().getPlugin("SOYSMonthlyCard");
 * plugin.getApi().registerPreClaimHook((player, tier) ->
 *     player.getWorld().getName().equals("spawn") ? HookResult.deny("出生世界不可领取") : HookResult.allow());
 * </pre>
 */
public final class SOYSMonthlyCardAPI {

    private final SOYSMonthlyCard plugin;
    private final List<PreClaimHook> hooks = new CopyOnWriteArrayList<>();

    public SOYSMonthlyCardAPI(SOYSMonthlyCard plugin) {
        this.plugin = plugin;
    }

    /** 注册一个领取前校验钩子（可多次注册，任一拒绝即拦截该档位） */
    public void registerPreClaimHook(PreClaimHook hook) {
        if (hook != null) {
            hooks.add(hook);
        }
    }

    /** 已注册的钩子（只读） */
    public List<PreClaimHook> getPreClaimHooks() {
        return Collections.unmodifiableList(hooks);
    }

    /**
     * 依次咨询所有钩子；任一拒绝即返回该拒绝结果，否则放行。
     * 钩子抛异常时按放行处理（不影响正常领取），仅记录警告。
     */
    public HookResult check(Player player, String tier) {
        for (PreClaimHook hook : hooks) {
            try {
                HookResult result = hook.beforeClaim(player, tier);
                if (result != null && !result.isAllowed()) {
                    return result;
                }
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING,
                        "领取前钩子执行异常（已忽略）: " + t.getMessage(), t);
            }
        }
        return HookResult.allow();
    }
}
