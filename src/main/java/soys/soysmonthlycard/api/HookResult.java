package soys.soysmonthlycard.api;

/**
 * 领取前钩子的判定结果。
 */
public final class HookResult {

    private static final HookResult ALLOW = new HookResult(true, null);

    private final boolean allow;
    private final String reason;

    private HookResult(boolean allow, String reason) {
        this.allow = allow;
        this.reason = reason == null ? "" : reason;
    }

    /** 放行（单例，可复用） */
    public static HookResult allow() {
        return ALLOW;
    }

    /** 拦截，附拒绝原因（会展示给玩家） */
    public static HookResult deny(String reason) {
        return new HookResult(false, reason);
    }

    public boolean isAllowed() {
        return allow;
    }

    public String getReason() {
        return reason;
    }
}
