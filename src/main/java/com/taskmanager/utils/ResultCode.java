package com.taskmanager.utils;

import lombok.Getter;

/**
 * 预设了各种常见响应状态的枚举类
 */
@Getter
public enum ResultCode {

    // 1**
    CONTINUE(100,"客户端应继续发送请求体"),
    SWITCHING_PROTOCOLS(101,"服务器应客户端请求切换协议"),
    PROCESSING(102,"服务器正在处理请求，暂无响应"),
    EARLY_HINTS(103,"在最终响应前返回部分头信息，让客户端预加载"),

    // 2**
    OK(200,"请求成功，响应体包含结果"),
    CREATED(201,"请求成功并创建了新资源"),
    ACCEPTED(202,"请求已接受，但尚未处理完成（异步）"),
    NO_CONTENT(204,"请求成功，但无响应体"),

    // 3**
    MULTIPLE_CHOICES(300,"请求有多个可选响应"),
    MOVED_PERMANENTLY(301,"资源永久重定向"),
    FOUND(302,"资源临时重定向"),

    // 4**
    BAD_REQUEST(400,"请求语法错误或参数无效"),
    UNAUTHORIZED(401,"未认证，需要登录"),
    NOT_EXIST(404,"资源不存在"),
    METHOD_NOT_ALLOWED(405,"请求方法不被允许"),
    CONFLICT(409,"请求与当前资源状态冲突"),

    // 5**
    INTERNAL_SERVER_ERROR(500,"系统内部异常"),
    BAD_GATEWAY(502,"网关/代理收到无效响应"),
    SERVICE_UNAVAILABLE(503,"服务不可用（过载/维护）"),
    GATEWAY_TIMEOUT(504,"网关/代理超时"),

    USER_NOT_FOUND(1001,"用户名或密码错误");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}
