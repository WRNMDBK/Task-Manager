package com.taskmanager.rateLimit.adpter.annotation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * IP 接口限流的级别
 */
@Getter
@AllArgsConstructor
public enum IpLimitLevel {

    STRICT(30,60,300),
    NORMAL(60,60,180),
    LOOSE(120,60,60);

    private final long count;
    private final long windowSecond;
    private final long banSecond;

}
