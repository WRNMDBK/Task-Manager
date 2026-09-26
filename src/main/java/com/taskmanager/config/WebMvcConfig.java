package com.taskmanager.config;

import com.taskmanager.interceptor.AuthorizeInterceptor;
import com.taskmanager.interceptor.LimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthorizeInterceptor authorizeInterceptor;
    private final LimitInterceptor limitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 限流拦截器必须在校验拦截器之前，防止无效的恶意请求刷爆接口
        registry.addInterceptor(limitInterceptor)
                .addPathPatterns("/**")
                .order(1);

        registry.addInterceptor(authorizeInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login")
                .excludePathPatterns("/register/**")
                .order(2);

    }

}
