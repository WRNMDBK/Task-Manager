package com.taskmanager.identity.adapter.web;

import com.taskmanager.rateLimit.adpter.annotation.ipRateLimit;
import com.taskmanager.identity.adapter.web.dto.FirstRegisterRequest;
import com.taskmanager.identity.adapter.web.dto.SecondRegisterRequest;
import com.taskmanager.identity.application.AuthorizeService;
import com.taskmanager.identity.infrastructure.JWTUtils;
import com.taskmanager.shared.web.Result;
import com.taskmanager.rateLimit.adpter.annotation.enums.IpLimitLevel;
import com.taskmanager.rateLimit.adpter.annotation.enums.RequestType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegisterController {

    private final JWTUtils jwtUtils;
    private final AuthorizeService authorizeService;

    @ipRateLimit(type = RequestType.AUTHORIZE, level = IpLimitLevel.STRICT)
    @PostMapping("/send-code")
    public Result<Void> verifyUserInfo(@Valid @RequestBody FirstRegisterRequest firstRegisterRequest, HttpServletRequest request) {
        authorizeService.verifyInfo(firstRegisterRequest,request.getRemoteAddr());
        return Result.success();
    }

    @ipRateLimit(type = RequestType.AUTHORIZE, level = IpLimitLevel.STRICT)
    @PostMapping("/confirm")
    public Result<String> verifyCodeAndRegister(@Valid @RequestBody SecondRegisterRequest secondRegisterRequest, HttpServletRequest request) {
        return Result.success(authorizeService.verifyCodeAndRegister(secondRegisterRequest, request.getRemoteAddr()));
    }

}
