package com.taskmanager.identity.application;

import com.taskmanager.identity.adapter.web.dto.FirstRegisterRequest;
import com.taskmanager.identity.adapter.web.dto.LoginRequest;
import com.taskmanager.identity.adapter.web.dto.SecondRegisterRequest;

public interface AuthorizeService {

    /**
     * 登录
     * @param loginRequest 用户信息容器
     * @return Token 令牌
     */
    String manualLogin(LoginRequest loginRequest);

    /**
     * 接收用户信息并查重
     * @param firstRegisterRequest 用户信息对象
     */
    void verifyInfo(FirstRegisterRequest firstRegisterRequest, String ip);

    /**
     * 校验用户发来的验证码并注册
     * @param secondRegisterRequest 六位数验证码
     * @return JWT 令牌
     */
    String verifyCodeAndRegister(SecondRegisterRequest secondRegisterRequest, String ip);

    /**
     * 退出登录
     * @param jwt JWT 令牌
     */
    void logout(String jwt);

}
