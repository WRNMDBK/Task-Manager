package com.taskmanager.controller.authorize;

import com.taskmanager.annotation.RateLimit;
import com.taskmanager.entity.vo.FirstRegisterVO;
import com.taskmanager.entity.vo.SecondRegisterVO;
import com.taskmanager.service.AuthorizeService;
import com.taskmanager.utils.JWTUtils;
import com.taskmanager.utils.Result;
import com.taskmanager.utils.enums.RequestType;
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

    @RateLimit(type = RequestType.AUTHORIZE, count = 5, second = 60)
    @PostMapping("/send-code")
    public Result<Void> verifyUserInfo(@Valid @RequestBody FirstRegisterVO firstRegisterVO, HttpServletRequest request) {
        authorizeService.verifyInfo(firstRegisterVO,request.getRemoteAddr());
        return Result.success();
    }

    @RateLimit(type = RequestType.AUTHORIZE, count = 5, second = 60)
    @PostMapping("/confirm")
    public Result<String> verifyCodeAndRegister(@Valid @RequestBody SecondRegisterVO secondRegisterVO, HttpServletRequest request) {
        return Result.success(authorizeService.verifyCodeAndRegister(secondRegisterVO, request.getRemoteAddr()));
    }

}
