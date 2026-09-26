package com.taskmanager.interceptor;

import com.taskmanager.utils.RequestLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用于接口限流的拦截器
 */
@RequiredArgsConstructor
@Component
public class LimitInterceptor implements HandlerInterceptor {

    private final RequestLimiter requestLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        requestLimiter.acquireOrThrow(request, handler);  // 对频繁请求限流（为了防止用户刷新页面被误封，必须放在跨域放行后面）
        return true;
    }
}
