package com.example.wms.admin.security;

/**
 * Request-scoped holder populated by {@link AuthInterceptor} before the controller method (and any
 * AOP advice around it, e.g. {@code PermissionAspect}) runs. Must be cleared at the end of every
 * request since the servlet container reuses threads across requests.
 */
public final class CurrentUserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(CurrentUser currentUser) {
        HOLDER.set(currentUser);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
