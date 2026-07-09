package com.example.wms.admin.aspect;

import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.admin.security.CurrentUser;
import com.example.wms.admin.security.CurrentUserContext;
import com.example.wms.admin.service.SysOperationLogAsyncService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
public class SysOperationAspect {

    private static final Logger log = LoggerFactory.getLogger(SysOperationAspect.class);

    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    private final SysOperationLogAsyncService sysOperationLogAsyncService;

    public SysOperationAspect(SysOperationLogAsyncService sysOperationLogAsyncService) {
        this.sysOperationLogAsyncService = sysOperationLogAsyncService;
    }

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, SysOperationLog operationLog) throws Throwable {
        Object result = null;
        boolean success = true;
        String errorMessage = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            success = false;
            errorMessage = ex.getMessage();
            throw ex;
        } finally {
            // Read here, after proceed() has returned/thrown, so the business method has already
            // generated and saved its number; getAndClear() also wipes the ThreadLocal so it can
            // never leak into a later request on this thread.
            OperationLogContext.Holder context = OperationLogContext.getAndClear();

            String bizNo = resolveBizNo(joinPoint, result, operationLog.bizNo());
            if (!StringUtils.hasText(bizNo) && StringUtils.hasText(context.getBizNo())) {
                bizNo = context.getBizNo();
            }

            String bizType = operationLog.bizType();
            if (!StringUtils.hasText(bizType) && StringUtils.hasText(context.getBizType())) {
                bizType = context.getBizType();
            }

            Long bizId = resolveBizId(joinPoint, result, operationLog.bizId());
            if (bizId == null) {
                bizId = context.getBizId();
            }

            try {
                CurrentUser currentUser = CurrentUserContext.get();
                sysOperationLogAsyncService.saveLog(
                        currentUser != null ? currentUser.userId() : null,
                        currentUser != null ? currentUser.username() : "anonymous",
                        operationLog.operationType(),
                        operationLog.module(),
                        bizNo,
                        bizType,
                        bizId,
                        operationLog.content(),
                        getRequestUri(),
                        getRequestMethod(),
                        success,
                        errorMessage,
                        getClientIp()
                );
            } catch (RuntimeException ex) {
                log.warn("submit operation log task failed, operationType={}, bizNo={}",
                        operationLog.operationType(), bizNo, ex);
            }
        }
    }

    private String resolveBizNo(ProceedingJoinPoint joinPoint, Object result, String bizNoExpression) {
        if (StringUtils.hasText(bizNoExpression)) {
            try {
                String bizNo = parseExpression(joinPoint, result, bizNoExpression);
                if (StringUtils.hasText(bizNo)) {
                    return bizNo;
                }
            } catch (RuntimeException ex) {
                log.warn("resolve operation log bizNo failed, expression={}", bizNoExpression, ex);
            }
        }
        return resolveOrderNoFromResult(result);
    }

    private Long resolveBizId(ProceedingJoinPoint joinPoint, Object result, String bizIdExpression) {
        if (StringUtils.hasText(bizIdExpression)) {
            try {
                Object value = parseExpressionRaw(joinPoint, result, bizIdExpression);
                if (value instanceof Long longValue) {
                    return longValue;
                }
                if (value instanceof Number number) {
                    return number.longValue();
                }
            } catch (RuntimeException ex) {
                log.warn("resolve operation log bizId failed, expression={}", bizIdExpression, ex);
            }
        }
        Object data = invokeNoArgMethod(result, "data");
        Object id = invokeNoArgMethod(data, "id");
        return id instanceof Long longValue ? longValue : null;
    }

    private String parseExpression(ProceedingJoinPoint joinPoint, Object result, String expression) {
        Object value = parseExpressionRaw(joinPoint, result, expression);
        return value == null ? "" : String.valueOf(value);
    }

    private Object parseExpressionRaw(ProceedingJoinPoint joinPoint, Object result, String expression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);

        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("result", result);
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        return expressionParser.parseExpression(expression).getValue(context);
    }

    private String resolveOrderNoFromResult(Object result) {
        Object data = invokeNoArgMethod(result, "data");
        Object orderNo = invokeNoArgMethod(data, "orderNo");
        return orderNo == null ? "" : String.valueOf(orderNo);
    }

    private Object invokeNoArgMethod(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String getRequestUri() {
        HttpServletRequest request = currentRequest();
        return request != null ? request.getRequestURI() : "";
    }

    private String getRequestMethod() {
        HttpServletRequest request = currentRequest();
        return request != null ? request.getMethod() : "";
    }

    private String getClientIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return "";
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }

        return request.getRemoteAddr();
    }
}
