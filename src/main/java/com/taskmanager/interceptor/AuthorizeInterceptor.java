package com.taskmanager.interceptor;

import com.taskmanager.entity.dto.LoginUserInfo;
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

/**
 * 用于校验的拦截器
 */
@RequiredArgsConstructor
@Component
public class AuthorizeInterceptor implements HandlerInterceptor {

    private final JWTUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (HttpMethod.OPTIONS.equals(request.getMethod())) return true;  // 跨域预检请求直接放行
        LoginUserInfo loginUserInfo = jwtUtils.parseAndVerifyJwt(request.getHeader("Authorization"));  // 解析校验 Token
        if(stringRedisTemplate.hasKey("jwt:logout:" + loginUserInfo.getUuid())) throw new BusinessException(ResultCode.UNAUTHORIZED);  // 检查是否已经退出登录
        request.setAttribute("user",loginUserInfo);
        return true;
    }

}
