package com.taskmanager;

import com.taskmanager.exception.BusinessException;
import com.taskmanager.utils.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TaskManagerApplicationTests {

    @Test
    void contextLoads() {
        BusinessException exception = new BusinessException(ResultCode.OK);
        exception.initCause(new RuntimeException());
        System.out.println("程序出现异常，以下为异常的详细信息：");
        System.out.println("detailMessage: " + exception.getMessage());
        System.out.println("cause: " + exception.getCause());
        System.out.println("localizedMessage: " + exception.getLocalizedMessage());
    }

}
