package com.taskmanager.interceptor;

import com.taskmanager.exception.BusinessException;
import com.taskmanager.utils.JWTUtils;
import com.taskmanager.utils.enums.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class AuthorizeInterceptor implements HandlerInterceptor {

    private final JWTUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (HttpMethod.OPTIONS.equals(request.getMethod())) return true;  // 跨域预检请求直接放行
        String token = request.getHeader("Authorization");
        if(stringRedisTemplate.hasKey("jwt:blackList:" + token)) throw new BusinessException(ResultCode.UNAUTHORIZED);
        Map<String, Object> userInfo = jwtUtils.parseAndVerifyJwt(token);
        request.setAttribute("user",userInfo);
        return true;
    }

}
