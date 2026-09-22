package com.taskmanager.service.impl;

import com.taskmanager.entity.dto.User;
import com.taskmanager.entity.vo.RequestUser;
import com.taskmanager.exception.BusinessException;
import com.taskmanager.mapper.UserMapper;
import com.taskmanager.service.AuthorizeService;
import com.taskmanager.utils.BCryptUtils;
import com.taskmanager.utils.JWTUtils;
import com.taskmanager.utils.enums.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorizeServiceImpl implements AuthorizeService {

    private final JWTUtils jwtUtils;
    private final BCryptUtils bCryptUtils;
    private final UserMapper userMapper;

    public String manualLogin(RequestUser userVO) {
        String username = userVO.getUsername();
        String password = userVO.getPassword();
        User user = userMapper.selectUserByUsername(username);
        if (user == null) throw new BusinessException(ResultCode.BAD_REQUEST,"该用户不存在，请重新输入用户名");
        boolean match = bCryptUtils.match(password, user.getPassword());
        if (!match) throw new BusinessException(ResultCode.BAD_REQUEST,"密码错误，登录失败");
        return jwtUtils.createJwt(user.getId(), username);
    }

    public String register(RequestUser user) {

        return null;
    }

    public void logout() {}

}
