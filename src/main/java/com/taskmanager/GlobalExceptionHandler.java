package com.taskmanager;

import com.taskmanager.exception.BusinessException;
import com.taskmanager.utils.Result;
import com.taskmanager.utils.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 捕获自定义业务异常（HTTP 200，但业务码非 200）
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage()); // 不打堆栈，只记录警告
        return Result.failure(e.getResultCode(), e.getMessage());
    }

    // 2. 兜底捕获所有系统异常
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常：", e); // 必须打堆栈！方便排查！
        return Result.failure(ResultCode.INTERNAL_SERVER_ERROR);
    }

}
