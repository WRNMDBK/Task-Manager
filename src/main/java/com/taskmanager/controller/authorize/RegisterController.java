package com.taskmanager.controller.authorize;

import com.taskmanager.entity.vo.FirstRegisterVO;
import com.taskmanager.entity.vo.SecondRegisterVO;
import com.taskmanager.service.AuthorizeService;
import com.taskmanager.utils.JWTUtils;
import com.taskmanager.utils.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegisterController {

    private final JWTUtils jwtUtils;
    private final AuthorizeService authorizeService;

    @PostMapping("/send-code")
    public Result<Void> verifyUserInfo(@Valid @RequestBody FirstRegisterVO firstRegisterVO) {
        authorizeService.verifyInfo(firstRegisterVO);
        return Result.success();
    }

    @PostMapping("/confirm")
    public Result<String> verifyCodeAndRegister(@Valid @RequestBody SecondRegisterVO secondRegisterVO) {
        return Result.success(authorizeService.verifyCodeAndRegister(secondRegisterVO));
    }

}
