package com.taskmanager.utils.enums;

import lombok.Getter;

/**
 * 定义了请求的类型，用于接口限流
 */
@Getter
public enum RequestType {

    AUTHORIZE("authorize"),
    CREATE_TASK("createTask"),
    GET_TASK_MESSAGE("getTaskMessage"),
    GET_TASK_LIST("getTaskList"),
    COMPLETE_TASK("completeTask"),
    DELETE_TASK("deleteTask");

    private final String type;

    RequestType(String type) {
        this.type = type;
    }

}
