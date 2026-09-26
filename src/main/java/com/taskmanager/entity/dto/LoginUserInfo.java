package com.taskmanager.entity.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class LoginUserInfo {
    private Long uid;
    private String username;
    private Date expireTime;
    private String uuid;
}
