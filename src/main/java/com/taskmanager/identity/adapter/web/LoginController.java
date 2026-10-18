package com.taskmanager.identity.adapter.web;

import com.taskmanager.rateLimit.adpter.annotation.ipRateLimit;
import com.taskmanager.identity.adapter.web.dto.LoginRequest;
import com.taskmanager.identity.application.AuthorizeService;
import com.taskmanager.shared.web.Result;
import com.taskmanager.rateLimit.adpter.annotation.enums.IpLimitLevel;
import com.taskmanager.rateLimit.adpter.annotation.enums.RequestType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/login")
@RequiredArgsConstructor
public class LoginController {

    private final AuthorizeService authorizeService;

    @ipRateLimit(type = RequestType.AUTHORIZE, level = IpLimitLevel.STRICT)
    @PostMapping
    public Result<String> manualLogin(@Valid @RequestBody LoginRequest loginRequest) {
        return Result.success(authorizeService.manualLogin(loginRequest));
    }

}
