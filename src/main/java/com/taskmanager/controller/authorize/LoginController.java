package com.taskmanager.controller.authorize;

import com.taskmanager.annotation.RateLimit;
import com.taskmanager.entity.vo.LoginVO;
import com.taskmanager.entity.vo.SecondRegisterVO;
import com.taskmanager.service.AuthorizeService;
import com.taskmanager.utils.Result;
import com.taskmanager.utils.enums.RequestType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/login")
@RequiredArgsConstructor
public class LoginController {

    private final AuthorizeService authorizeService;

    @RateLimit(type = RequestType.AUTHORIZE, count = 5, second = 60)
    @PostMapping
    public Result<String> manualLogin(@Valid @RequestBody LoginVO loginVO) {
        return Result.success(authorizeService.manualLogin(loginVO));
    }

}
