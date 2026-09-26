package com.taskmanager.utils;

import com.taskmanager.entity.dto.LoginUserInfo;
import com.taskmanager.exception.BusinessException;
import com.taskmanager.utils.enums.ResultCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用于生成、解析、校验 JWT 的工具类
 */
@Component
public class JWTUtils {

    private final Integer expirationDay;  // 过期天数
    private final SecretKey secretKey;  // 密钥
    private final JwtParser jwtParser;  // JWT 解析器

    @Autowired
    public JWTUtils(@Value("${jwt.key}") String key, @Value("${jwt.expiration-day}") Integer expirationDay) {
        this.expirationDay = expirationDay;
        this.secretKey = Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parser().verifyWith(secretKey).build();
    }

    /**
     * 生成包含'用户id'和'用户名'的Token
     * @param uid 用户id
     * @param username 用户名
     * @return Token 令牌
     */
    public String createJwt(Long uid,String username) {
        Map<String,Object> claims = Map.of("uid",uid,"username",username);
        return Jwts.builder()
                .claims(claims)  // 设置负载的自定义内容
                .issuedAt(new Date())  // 设置签发时间
                .id(UUID.randomUUID().toString())  // 设置唯一 ID
                .expiration(new Date(System.currentTimeMillis() + (long) 24L * 3600 * 1000 * expirationDay))  // 设置过期时间（可在 yml 中更改天数）
                .signWith(secretKey,Jwts.SIG.HS256)  // 使用 HS256 算法签名
                .compact();  // 打包压缩成字符串
    }

    /**
     * 解析并校验Token
     * @param token 从请求头获取的 Token 令牌
     * @return 若解析成功则返回包含'用户id'、'用户名'和'过期时间'的 LoginUserInfo 对象
     */
    public LoginUserInfo parseAndVerifyJwt(String token) {
        if (token == null || token.isEmpty() || !token.startsWith("Bearer ")) throw new BusinessException(ResultCode.UNAUTHORIZED,"未提供Token");
        try {
            String jwt = token.trim().substring(7);
            Jws<Claims> claims = jwtParser.parseSignedClaims(jwt);
            return LoginUserInfo.builder().
                    uid(claims.getPayload().get("uid", Long.class)).  // JWT存的是JSON，拿出来时uid自动变成Integer，使用(Long)转换会异常，因此改成JJWT提供的带类型读取get()
                    username((String) claims.getPayload().get("username")).
                    expireTime(claims.getPayload().getExpiration()).
                    uuid(claims.getPayload().getId()).build();
            // 按照由小到大顺序先抓子类异常再抓父类异常
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED,"Token已过期");
        } catch (MalformedJwtException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED,"Token格式错误");
        } catch (io.jsonwebtoken.security.SecurityException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED,"Token签名错误");
        } catch (JwtException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
            //  兜底的 catch 不能用 服务器内部错误 500。
            //  因为 JwtException 家族的所有异常，本质都是「你给的凭证有问题」，不是「服务器坏了」。
            //  返回 500 会让前端误判为「系统异常」，而不是「请重新登录」。所以兜底也应该是 UNAUTHORIZED。
        }
    }
}
