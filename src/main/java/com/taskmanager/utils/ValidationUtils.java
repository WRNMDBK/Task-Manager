package com.taskmanager.utils;

import jakarta.validation.ConstraintViolationException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;

/**
 * 统一提取校验失败的信息
 * 无论传进来的是 Spring 的异常还是 Jakarta 的异常，都通过'ValidationUtils.getErrorMessage(Exception e)'获取内部信息
 */
@Component
public class ValidationUtils {

    public static String getErrorMessage(Exception e) {
        if (e instanceof MethodArgumentNotValidException ex) {
            return ex.getBindingResult().getFieldError().getDefaultMessage();
        } else if (e instanceof ConstraintViolationException ex) {
            return ex.getConstraintViolations().iterator().next().getMessage();
        }
        return "参数校验错误";
    }

}
