package com.taskmanager.utils;

import com.taskmanager.annotation.RateLimit;
import com.taskmanager.exception.BusinessException;
import com.taskmanager.utils.enums.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.method.HandlerMethod;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RequestLimiter {

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Boolean> defaultRedisScript;

    public void acquireOrThrow(HttpServletRequest request, Object objectHandler) {
        if (objectHandler instanceof HandlerMethod handler) {
            RateLimit rateLimit = handler.getMethodAnnotation(RateLimit.class);
            if (rateLimit == null) return;  // 没有被限流注解标记的接口直接放行

            // 初始化变量
            long count = rateLimit.count();
            long second = rateLimit.second();
            long banSecond = rateLimit.banSecond();
            String key_blackList = "limit:blacklist:" + rateLimit.type().getType() + ":" + request.getRemoteAddr();
            String key_counter = "limit:count:" + rateLimit.type().getType() + ":" + request.getRemoteAddr();

            // 使用 Lua 脚本原子化执行
            // 为什么用 Lua：分几次发命令会被并发请求插队，也可能因为异常/重启只发出去一半
            // （留下一个永不过期的计数键 → 那个 IP 就被永久封死了）；
            // Lua 整段在 Redis 端一口气跑完，既不会被插队，还能拿前一步的结果决定下一步。
            Boolean result = stringRedisTemplate.execute(defaultRedisScript,
                    List.of(key_blackList, key_counter),
                    String.valueOf(count),
                    String.valueOf(second),
                    String.valueOf(banSecond));
            if (Boolean.FALSE.equals(result)) throw new BusinessException(ResultCode.TOO_MANY_REQUESTS);
        }
    }

}
