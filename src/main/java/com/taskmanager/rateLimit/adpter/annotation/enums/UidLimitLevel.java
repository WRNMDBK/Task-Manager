package com.taskmanager.rateLimit.adpter.annotation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * UID 接口限流的级别
 */
@Getter
@AllArgsConstructor
public enum UidLimitLevel {

    STRICT(3,60,300),
    NORMAL(10,60,180),
    LOOSE(20,60,60);

    private final long count;
    private final long windowSecond;
    private final long banSecond;

}
