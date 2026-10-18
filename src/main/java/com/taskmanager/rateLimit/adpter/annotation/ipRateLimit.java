package com.taskmanager.rateLimit.adpter.annotation;

import com.taskmanager.rateLimit.adpter.annotation.enums.IpLimitLevel;
import com.taskmanager.rateLimit.adpter.annotation.enums.RequestType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于在 Controller 接口上定义 IP 限流规则
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ipRateLimit {
    RequestType type();
    IpLimitLevel level() default IpLimitLevel.NORMAL;
}
