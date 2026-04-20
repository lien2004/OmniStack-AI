package com.lingnong.user.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lingnong.user.entity.User;
import com.lingnong.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(ApplicationArguments args) {
        initUser("admin", "admin123", 1, "13800138001");
        initUser("user", "123456", 0, "13800138002");
    }

    private void initUser(String username, String rawPassword, int role, String mobile) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User existing = userMapper.selectOne(wrapper);

        if (existing == null) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setRole(role);
            user.setMobile(mobile);
            userMapper.insert(user);
            log.info("初始化用户 [{}] 成功", username);
        } else {
            // 更新密码确保 BCrypt 正确
            existing.setPassword(passwordEncoder.encode(rawPassword));
            userMapper.updateById(existing);
            log.info("更新用户 [{}] 密码成功", username);
        }
    }
}
