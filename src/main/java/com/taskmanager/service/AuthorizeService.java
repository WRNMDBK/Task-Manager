package com.taskmanager.service;

import com.taskmanager.entity.vo.FirstRegisterVO;
import com.taskmanager.entity.vo.LoginVO;
import com.taskmanager.entity.vo.SecondRegisterVO;

public interface AuthorizeService {

    /**
     * 登录
     * @param loginVO 用户信息容器
     * @return Token 令牌
     */
    String manualLogin(LoginVO loginVO);

    /**
     * 接收用户信息并查重
     * @param firstRegisterVO 用户信息对象
     */
    void verifyInfo(FirstRegisterVO firstRegisterVO);

    /**
     * 校验用户发来的验证码并注册
     * @param secondRegisterVO 六位数验证码
     * @return JWT 令牌
     */
    String verifyCodeAndRegister(SecondRegisterVO secondRegisterVO);

    /**
     * 退出登录
     * @param jwt JWT 令牌
     */
    void logout(String jwt);

}
