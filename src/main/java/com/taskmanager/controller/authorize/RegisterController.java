package com.taskmanager.controller.authorize;

import com.taskmanager.entity.vo.RequestUser;
import com.taskmanager.utils.JWTUtils;
import com.taskmanager.utils.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegisterController {

    private final JWTUtils jwtUtils;

    @PostMapping
    public Result<Void> register(@Valid @RequestBody RequestUser user) {

        return null;
    }
}
