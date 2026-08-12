package soys.soysmonthlycard.api;

import org.bukkit.entity.Player;

/**
 * 领取前校验钩子。其他插件可注册此钩子，在玩家实际领取某一档位奖励前被调用，
 * 通过返回 {@link HookResult#deny(String)} 拦截发放，或 {@link HookResult#allow()} 放行。
 * <p>所有已注册钩子将被依次咨询，任一拒绝即拦截该档位（其余档位不受影响）。</p>
 */
@FunctionalInterface
public interface PreClaimHook {

    /**
     * 在发放某档位前调用。
     *
     * @param player 领取的玩家
     * @param tier   档位名（对应 rewards.yml 的 tiers.&lt;tier&gt;）
     * @return 放行或拦截结果
     */
    HookResult beforeClaim(Player player, String tier);
}
