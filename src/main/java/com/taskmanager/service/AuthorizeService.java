package com.taskmanager.service;

import com.taskmanager.entity.vo.RequestUser;

public interface AuthorizeService {

    /**
     * 登录
     * @param user 用户信息容器
     * @return Token 令牌
     */
    String manualLogin(RequestUser user);

}
