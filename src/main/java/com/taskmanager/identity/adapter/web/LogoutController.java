package com.taskmanager.identity.adapter.web;

import com.taskmanager.identity.application.AuthorizeService;
import com.taskmanager.shared.web.Result;
import com.taskmanager.shared.web.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/logout")
@RequiredArgsConstructor
public class LogoutController {

    private final AuthorizeService authorizeService;

    @GetMapping
    public Result<Void> logout(HttpServletRequest request) {
        authorizeService.logout(request.getHeader("Authorization"));
        return Result.success(ResultCode.NO_CONTENT, null);
    }

}
