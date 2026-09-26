package com.taskmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class RedisScriptsConfig {

    @Bean("rate-limit")
    public DefaultRedisScript<Boolean> defaultRedisScript() {
        DefaultRedisScript<Boolean> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/rate-limit.lua"));
        script.setResultType(Boolean.class);
        return script;
    }

}
