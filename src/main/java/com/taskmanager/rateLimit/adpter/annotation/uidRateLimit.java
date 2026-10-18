package com.taskmanager.rateLimit.adpter.annotation;

import com.taskmanager.rateLimit.adpter.annotation.enums.RequestType;
import com.taskmanager.rateLimit.adpter.annotation.enums.UidLimitLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于在 Controller 接口上定义 UID 限流规则
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface uidRateLimit {
    RequestType type();
    UidLimitLevel level() default UidLimitLevel.NORMAL;
}
