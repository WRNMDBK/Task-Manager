package com.taskmanager.utils;

import com.taskmanager.utils.enums.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 预设了各种状况的结果类型
 * 使用 com.taskmanager.utils.enums.ResultCode 的枚举对象和属性作为预设
 * @param <T> 泛型响应数据的类型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    // 无携带信息的成功响应
    public static <T> Result<T> success() {
            return new Result<>(ResultCode.NO_CONTENT.getCode(), ResultCode.NO_CONTENT.getMessage(), null);
    }

    // 携带信息的成功响应
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.OK.getCode(), ResultCode.OK.getMessage(), data);
    }

    // 完全自定义的成功响应
    public static <T> Result<T> success(ResultCode resultCode,T data) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(),data);
    }

    // 预设信息的错误响应
    public static <T> Result<T> failure(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    // 自定义信息的错误响应
    public static <T> Result<T> failure(ResultCode resultCode, String failureMessage) {
        return new Result<>(resultCode.getCode(), failureMessage,null);
    }
}
