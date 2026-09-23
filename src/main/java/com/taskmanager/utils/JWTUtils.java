package com.taskmanager.utils;

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

/**
 * 用于生成、解析、校验 JWT 的工具类
 */
@Component
public class JWTUtils {

    private final Integer expirationDay;  // 过期天数
    private final SecretKey secretKey;  // 密钥

    @Autowired
    public JWTUtils(@Value("${jwt.key}") String key, @Value("${jwt.expiration-day}") Integer expirationDay) {
        this.expirationDay = expirationDay;
        this.secretKey = Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
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
                .expiration(new Date(System.currentTimeMillis() + (long) 24L * 3600 * 1000 * expirationDay))  // 设置过期时间（可在 yml 中更改天数）
                .signWith(secretKey,Jwts.SIG.HS256)  // 使用 HS256 算法签名
                .compact();  // 打包压缩成字符串
    }

    /**
     * 解析并校验Token
     * @param token 从请求头获取的 Token 令牌
     * @return 若解析成功则返回包含'用户id'和'用户名'的 Map
     */
    public Map<String, Object> parseAndVerifyJwt(String token) {
        if (token == null || token.trim().isEmpty() || !token.startsWith("Bearer ")) throw new BusinessException(ResultCode.UNAUTHORIZED,"未提供Token");
        try {
            String jwt = token.substring(7);
            Jws<Claims> claims = Jwts.parser()
                    .verifyWith(secretKey)  // 用密钥验签
                    .build()  // 造出解析器
                    .parseSignedClaims(jwt);  // 解析并校验
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("uid",claims.getPayload().get("uid"));
            userInfo.put("username",claims.getPayload().get("username"));
            userInfo.put("expire-time",claims.getPayload().getExpiration());
            return userInfo;
            // 按照由小到大顺序先抓子类异常再抓父类异常
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED,"Token已过期");
        } catch (MalformedJwtException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED,"Token格式错误");
        } catch (io.jsonwebtoken.security.SecurityException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED,"Token签名错误");
        } catch (JwtException e) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "Token解析失败");
        }
    }
}
