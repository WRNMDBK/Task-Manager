package com.taskmanager.entity.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SecondRegisterVO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 15, message = "用户名的长度必须在3到15之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 5, max = 30, message = "密码长度必须在5到30之间")
    private String password;

    @NotBlank(message = "电子邮件地址不能为空")
    @Email(message = "请填写正确的电子邮件地址")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "格式错误，验证码必须是6位数字")
    private String code;

}
