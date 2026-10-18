package com.taskmanager.rateLimit.adpter.interceptor;

import com.taskmanager.rateLimit.infrastructure.uidLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 基于 uid 的接口限流拦截器
 */
@Component
@RequiredArgsConstructor
public class uidLimitInterceptor implements HandlerInterceptor {

    private final uidLimiter uidLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        uidLimiter.acquireOrThrow(request, handler);
        return true;
    }
}
