package com.taskmanager.rateLimit.infrastructure;

import com.taskmanager.rateLimit.adpter.annotation.uidRateLimit;
import com.taskmanager.identity.domain.LoginUserInfo;
import com.taskmanager.shared.exception.BusinessException;
import com.taskmanager.shared.web.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.List;

@Component
@RequiredArgsConstructor
public class uidLimiter {

    @Qualifier("rate-limit-uid")
    private final DefaultRedisScript<Boolean> uidLimitScript;
    private final StringRedisTemplate stringRedisTemplate;

    public void acquireOrThrow(HttpServletRequest request, Object objectHandler) {

        if (objectHandler instanceof HandlerMethod handler) {
            uidRateLimit uidRateLimit = handler.getMethodAnnotation(uidRateLimit.class);
            if (uidRateLimit == null) return;
            LoginUserInfo user = (LoginUserInfo) request.getAttribute("user");
            if (user == null) throw new BusinessException(ResultCode.UNAUTHORIZED);
            // 初始化参数
            long uid = user.getUid();
            long count = uidRateLimit.level().getCount();
            long windowSecond = uidRateLimit.level().getWindowSecond();
            long banSecond = uidRateLimit.level().getBanSecond();
            String key_blackList = "limit:uid:blacklist:" + uidRateLimit.type().getType() + ":" + uid;
            String key_count = "limit:uid:count:" + uidRateLimit.type().getType() + ":" + uid;
            // 使用 lua 脚本原子化执行限流操作
            Boolean result = stringRedisTemplate.execute(uidLimitScript,
                    List.of(key_blackList, key_count),
                    String.valueOf(count),
                    String.valueOf(windowSecond),
                    String.valueOf(banSecond));
            if (!Boolean.TRUE.equals(result)) throw new BusinessException(ResultCode.TOO_MANY_REQUESTS);
        }

    }

}
