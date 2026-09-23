package com.taskmanager.entity.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
public class CreateTaskVO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题不能超过100个字符")
    private String title;

    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;

}
