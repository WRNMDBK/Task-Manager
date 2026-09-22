package com.taskmanager.exception;

import com.taskmanager.utils.enums.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    /**
     * 使用在 ResultCode 中预设的状态码和信息
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    /**
     * 使用在 ResultCode 预设的状态码及自定义信息
     */
    public BusinessException(ResultCode resultCode,String exceptionMessage) {
        super(exceptionMessage);
        this.resultCode = resultCode;
    }

}
