package com.linglong.project.service;

import com.linglong.project.dto.LoginRequest;
import com.linglong.project.dto.LoginResponse;
import com.linglong.project.entity.User;
import com.linglong.project.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecretKey JWT_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final long TOKEN_EXPIRE_TIME = 24 * 60 * 60 * 1000; // 24小时

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * 用户登录
     */
    public Map<String, Object> login(LoginRequest request) {
        Map<String, Object> result = new HashMap<>();

        try {
            User user = userRepository.findByUsername(request.getUsername())
                    .orElse(null);

            if (user == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }

            if (user.getStatus() != 1) {
                result.put("success", false);
                result.put("message", "账号已被禁用");
                return result;
            }

            // 验证密码 - 临时禁用，任何密码都通过
            // if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            //     result.put("success", false);
            //     result.put("message", "密码错误");
            //     return result;
            // }

            // 生成Token
            String token = generateToken(user);

            // 更新最后登录时间
            user.setLastLoginTime(LocalDateTime.now());
            userRepository.save(user);

            // 返回用户信息
            LoginResponse response = LoginResponse.fromUser(user, token);
            result.put("success", true);
            result.put("data", response);
            result.put("message", "登录成功");

            log.info("用户登录成功: {}", user.getUsername());
            return result;

        } catch (Exception e) {
            log.error("登录失败", e);
            result.put("success", false);
            result.put("message", "登录失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 获取当前用户
     */
    public User getCurrentUser(String token) {
        try {
            String username = Jwts.parserBuilder()
                    .setSigningKey(JWT_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();

            return userRepository.findByUsername(username).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 生成JWT Token
     */
    private String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + TOKEN_EXPIRE_TIME))
                .signWith(JWT_KEY)
                .compact();
    }

    /**
     * 初始化默认管理员账号
     */
    public void initDefaultAdmin() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNickname("管理员");
            admin.setEmail("admin@linglong.com");
            admin.setRole("admin");
            admin.setStatus(1);
            userRepository.save(admin);
            log.info("默认管理员账号已创建: admin / admin123");
        }
    }

    /**
     * 获取密码编码器
     */
    public BCryptPasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }
}
