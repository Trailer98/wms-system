package com.example.wms.admin.aspect;

import java.util.HashMap;
import java.util.Map;

/**
 * Thread-local fallback for {@link com.example.wms.admin.annotation.SysOperationLog} fields that a
 * SpEL expression can't reach (e.g. the business number is computed deep inside a service call and
 * never appears on the controller's return value). A service method may call
 * {@code OperationLogContext.setBizNo(...)} before returning; {@code SysOperationAspect} reads it
 * only when the annotation's own expression resolved to nothing.
 * <p>
 * Not propagated to async/child threads on purpose: {@code getAndClear()} must be called on the
 * same request thread that set the values, in the aspect's {@code finally} block, so the ThreadLocal
 * never survives past one request even if a caller forgets to set anything.
 */
public final class OperationLogContext {

    private static final ThreadLocal<Holder> HOLDER = new ThreadLocal<>();

    private OperationLogContext() {
    }

    public static void setBizNo(String bizNo) {
        holder().bizNo = bizNo;
    }

    public static void setBizId(Long bizId) {
        holder().bizId = bizId;
    }

    public static void setBizType(String bizType) {
        holder().bizType = bizType;
    }

    public static void setExtra(String key, Object value) {
        holder().extra.put(key, value);
    }

    /** Reads and clears the current thread's context in one step; never returns null. */
    public static Holder getAndClear() {
        try {
            Holder holder = HOLDER.get();
            return holder != null ? holder : new Holder();
        } finally {
            HOLDER.remove();
        }
    }

    private static Holder holder() {
        Holder holder = HOLDER.get();
        if (holder == null) {
            holder = new Holder();
            HOLDER.set(holder);
        }
        return holder;
    }

    public static final class Holder {
        private String bizNo;
        private Long bizId;
        private String bizType;
        private final Map<String, Object> extra = new HashMap<>();

        public String getBizNo() {
            return bizNo;
        }

        public Long getBizId() {
            return bizId;
        }

        public String getBizType() {
            return bizType;
        }

        public Map<String, Object> getExtra() {
            return extra;
        }
    }
}
