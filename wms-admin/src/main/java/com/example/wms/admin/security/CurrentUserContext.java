package com.example.wms.admin.security;

/**
 * Request-scoped holder populated from Gateway identity headers before controller methods and AOP
 * advice run. Must be cleared at the end of every request because servlet threads are reused.
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
