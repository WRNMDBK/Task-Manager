package com.taskmanager.annotation;

import com.taskmanager.utils.enums.RequestType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于在 Controller 接口上定义限流规则的注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RateLimit {
    RequestType type();
    long count();
    long second();
    long banSecond() default 60;
}
