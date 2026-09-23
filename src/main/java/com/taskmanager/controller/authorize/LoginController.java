package com.taskmanager.controller.authorize;

import com.taskmanager.entity.vo.LoginVO;
import com.taskmanager.entity.vo.SecondRegisterVO;
import com.taskmanager.service.AuthorizeService;
import com.taskmanager.utils.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/login")
@RequiredArgsConstructor
public class LoginController {

    private final AuthorizeService authorizeService;

    @PostMapping
    public Result<String> manualLogin(@Valid @RequestBody LoginVO loginVO) {
        return Result.success(authorizeService.manualLogin(loginVO));
    }

}
