package com.taskmanager.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptUtils {

    private final BCryptPasswordEncoder encoder;

    public BCryptUtils(){
        this.encoder = new BCryptPasswordEncoder();
    }

    public String encode(String password) {
        return encoder.encode(password);
    }

    public boolean match(String originalPassword ,String encodedPassword) {
        return encoder.matches(originalPassword,encodedPassword);
    }

}
