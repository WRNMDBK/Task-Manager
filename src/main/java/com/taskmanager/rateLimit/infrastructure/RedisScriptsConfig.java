package com.taskmanager.rateLimit.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class RedisScriptsConfig {

    @Bean("rate-limit-ip")
    public DefaultRedisScript<Boolean> ipLimitScript() {
        DefaultRedisScript<Boolean> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/rate-limit-ip.lua"));
        script.setResultType(Boolean.class);
        return script;
    }

    @Bean("rate-limit-uid")
    public DefaultRedisScript<Boolean> uidLimitScript() {
        DefaultRedisScript<Boolean> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/rate-limit-uid.lua"));
        script.setResultType(Boolean.class);
        return script;
    }

}
