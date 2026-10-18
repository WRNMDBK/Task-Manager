package com.taskmanager.rateLimit.adpter.interceptor;

import com.taskmanager.rateLimit.infrastructure.ipLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 基于 IP 的接口限流的拦截器
 */
@RequiredArgsConstructor
@Component
public class ipLimitInterceptor implements HandlerInterceptor {

    private final ipLimiter ipLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        ipLimiter.acquireOrThrow(request, handler);  // 对频繁请求限流（为了防止用户刷新页面被误封，必须放在跨域放行后面）
        return true;
    }
}
