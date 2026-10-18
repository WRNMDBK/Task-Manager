package com.taskmanager.identity.application.impl;

import com.taskmanager.shared.mq.EmailMessageDTO;
import com.taskmanager.identity.domain.LoginUserInfo;
import com.taskmanager.identity.domain.User;
import com.taskmanager.identity.adapter.web.dto.FirstRegisterRequest;
import com.taskmanager.identity.adapter.web.dto.LoginRequest;
import com.taskmanager.identity.adapter.web.dto.SecondRegisterRequest;
import com.taskmanager.shared.exception.BusinessException;
import com.taskmanager.identity.infrastructure.UserMapper;
import com.taskmanager.identity.application.AuthorizeService;
import com.taskmanager.identity.infrastructure.BCryptUtils;
import com.taskmanager.identity.infrastructure.JWTUtils;
import com.taskmanager.shared.web.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthorizeServiceImpl implements AuthorizeService {

    private final JWTUtils jwtUtils;
    private final BCryptUtils bCryptUtils;
    private final UserMapper userMapper;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 登录
     * @param loginRequest 用户信息容器
     * @return Token 令牌
     */
    public String manualLogin(LoginRequest loginRequest) {
        // 初始化用户信息
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        // 检验该用户是否存在
        User user = userMapper.selectUserByUsername(username);
        if (user == null) throw new BusinessException(ResultCode.BAD_REQUEST,"该用户不存在，请重新输入用户名");
        // 校验 JWT 合法性
        boolean match = bCryptUtils.match(password, user.getPassword());
        if (!match) throw new BusinessException(ResultCode.BAD_REQUEST,"密码错误，登录失败");
        return jwtUtils.createJwt(user.getId(), username);
    }

    /**
     * 接收用户名和邮箱，先查重，再发送邮箱验证码
     * @param firstRegisterRequest 请求体的用户信息对象
     */
    public void verifyInfo(FirstRegisterRequest firstRegisterRequest, String ip) {
        // 初始化用户信息
        String username = firstRegisterRequest.getUsername();
        String email = firstRegisterRequest.getEmail();
        // 判断是否为频繁验证（由于返回值类型 Boolean 是包装类，有可能为空，必须用 equals 判断）
        Boolean isFirstTime = stringRedisTemplate.hasKey("EmailCode:" + email + ":" + ip);
        if (Boolean.TRUE.equals(isFirstTime)) throw new BusinessException(ResultCode.TOO_MANY_REQUESTS,"请求验证码频繁，请稍后再试");
        // 查询用户名和邮箱地址是否已存在
        if (userMapper.selectIdByUsername(username) != null) throw new BusinessException(ResultCode.CONFLICT, "该用户名已被使用");
        if (userMapper.selectIdByEmail(email) != null) throw new BusinessException(ResultCode.CONFLICT, "该电子邮件地址已被使用");
        // 发送验证码并存入 Redis
        String code = this.generateCode();
        rabbitTemplate.convertAndSend("amq.direct", "email", EmailMessageDTO.builder().email(email).code(code).ip(ip).build());
    }

    /**
     * 校验用户发来的验证码并注册
     * @param secondRegisterRequest 六位数验证码
     * @return JWT 令牌
     */
    public String verifyCodeAndRegister(SecondRegisterRequest secondRegisterRequest, String ip) {
        // 校验验证码
        String email = secondRegisterRequest.getEmail();
        String codeFromUser = secondRegisterRequest.getCode();
        this.verifyEmailCode(email,ip,codeFromUser);
        // 新增 user 信息
        String username = secondRegisterRequest.getUsername();
        String encodedPassword = bCryptUtils.encode(secondRegisterRequest.getPassword());
        User user = User.builder().username(username).password(encodedPassword).email(email).build();
        userMapper.insertUser(user);
        stringRedisTemplate.delete("EmailCode:" + email);  // 注册成功后清理验证码缓存
        // 生成 JWT 令牌并返回
        return jwtUtils.createJwt(user.getId(), user.getUsername());
    }

    /**
     * 退出登录
     * @param jwt JWT 令牌
     */
    public void logout(String jwt) {
        // 解析 JWT 拿到过期时间和 UUID（前提是 Token 还没过期）
        LoginUserInfo jwtInfo = jwtUtils.parseAndVerifyJwt(jwt);
        Date expireTime = jwtInfo.getExpireTime();
        // 算出剩余时间（毫秒）
        long ttlMillis = expireTime.getTime() - System.currentTimeMillis();
        // 如果还有剩余时间，就存入 Redis 的退出登录名单
        if (ttlMillis > 0) stringRedisTemplate.opsForValue().set("jwt:logout:"+jwtInfo.getUuid(),"1",Duration.ofMillis(ttlMillis));
    }

    /**
     * 生成六位数验证码
     * @return 验证码
     */
    private String generateCode() {
        // 生成一个 0 到 999999 之间的随机整数，用 %06d 格式化成 6 位
        int code = ThreadLocalRandom.current().nextInt(1000000);
        return String.format("%06d", code);
    }

    /**
     * 校验验证码
     */
    private void verifyEmailCode(String email, String ip,String codeFromUser) {
        String codeFromRedis = stringRedisTemplate.opsForValue().get("EmailCode:" + email + ":" + ip);
        if (codeFromRedis == null) throw new BusinessException(ResultCode.BAD_REQUEST,"验证码错误或已失效");
        if (!codeFromRedis.equals(codeFromUser)) throw new BusinessException(ResultCode.BAD_REQUEST,"验证码错误");
    }

}
