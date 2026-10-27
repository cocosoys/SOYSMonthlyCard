package soys.soysmonthlycard.exception;

/**
 * Web 层统一异常。
 *
 * <p>由 {@code spring.service.impl} 在鉴权失败 / 资源不存在 / 业务失败时抛出，
 * {@code web.controller} 仅负责按 {@link Kind} 捕获并映射为对应的 {@code AjaxResult}，
 * 不在控制器内编写任何业务逻辑。</p>
 */
public class WebException extends RuntimeException {

    /** 映射到 AjaxResult 的响应类型。 */
    public enum Kind {
        /** 未认证 / 凭证无效 → unauthorized */
        UNAUTHORIZED,
        /** 已认证但无权限（非 OP / 不在线）→ forbidden */
        FORBIDDEN,
        /** 资源不存在 → notFound */
        NOT_FOUND,
        /** 一般业务失败 → error */
        ERROR
    }

    private final Kind kind;

    public WebException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}
