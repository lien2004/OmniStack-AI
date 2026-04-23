package com.lingnong.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lingnong.user.common.BusinessException;
import com.lingnong.user.common.ResultCode;
import com.lingnong.user.dto.AdminUpdateUserRequest;
import com.lingnong.user.dto.ForgotPasswordRequest;
import com.lingnong.user.dto.LoginRequest;
import com.lingnong.user.dto.LoginResponse;
import com.lingnong.user.dto.RegisterRequest;
import com.lingnong.user.dto.SendCodeRequest;
import com.lingnong.user.dto.UpdateProfileRequest;
import com.lingnong.user.entity.User;
import com.lingnong.user.mapper.UserMapper;
import com.lingnong.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${user.sms.code-expire:300}")
    private Integer codeExpire;

    @Value("${user.sms.resend-interval:60}")
    private Integer resendInterval;

    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final String SMS_CODE_COUNT_PREFIX = "sms:count:";

    @Override
    public User register(RegisterRequest request) {
        if (StrUtil.isBlank(request.getUsername()) || StrUtil.isBlank(request.getPassword()) || StrUtil.isBlank(request.getMobile())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "参数不能为空");
        }

        if (!isValidMobile(request.getMobile())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "手机号格式不正确");
        }

        verifySmsCode(request.getMobile(), request.getVerifyCode());

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户名已存在");
        }

        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getMobile, request.getMobile());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "手机号已注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setMobile(request.getMobile());
        // 角色：0-普通用户，1-系统管理员，默认0
        user.setRole(request.getRole() != null ? request.getRole() : 0);

        userMapper.insert(user);
        log.info("用户注册成功: {}", user.getUsername());
        return user;
    }



    @Override
    public LoginResponse login(LoginRequest request) {
        if (StrUtil.isBlank(request.getUsername()) || StrUtil.isBlank(request.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户名和密码不能为空");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(User::getUsername, request.getUsername())
                .or()
                .eq(User::getMobile, request.getUsername()));
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户名或密码错误");
        }

        // 生成 token
        String token = UUID.randomUUID().toString().replace("-", "");
        String tokenKey = "token:" + token;
        redisTemplate.opsForValue().set(tokenKey, String.valueOf(user.getId()), 7, TimeUnit.DAYS);

        user.setPassword(null);

        LoginResponse response = new LoginResponse();
        response.setUser(user);
        response.setToken(token);
        return response;
    }
    @Override
    public String sendVerifyCode(SendCodeRequest request) {
        if (StrUtil.isBlank(request.getMobile()) || !isValidMobile(request.getMobile())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "手机号格式不正确");
        }

        String codeKey = SMS_CODE_PREFIX + request.getMobile();
        String countKey = SMS_CODE_COUNT_PREFIX + request.getMobile();

        String existingCode = redisTemplate.opsForValue().get(codeKey);
        if (StrUtil.isNotBlank(existingCode)) {
            Long ttl = redisTemplate.getExpire(codeKey, TimeUnit.SECONDS);
            long waitTime = resendInterval - (codeExpire - ttl);
            if (waitTime > 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "验证码已发送，请" + waitTime + "秒后重试");
            }
        }

        String count = redisTemplate.opsForValue().get(countKey);
        int sendCount = count != null ? Integer.parseInt(count) : 0;
        if (sendCount >= 5) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "发送次数过多，请稍后再试");
        }

        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));

        redisTemplate.opsForValue().set(codeKey, code, codeExpire, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(countKey, String.valueOf(sendCount + 1), 3600, TimeUnit.SECONDS);

        log.info("发送验证码到手机号: {}, 验证码: {}", request.getMobile(), code);
        return code;
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        if (StrUtil.isBlank(request.getMobile()) || StrUtil.isBlank(request.getVerifyCode()) || StrUtil.isBlank(request.getNewPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "参数不能为空");
        }

        if (request.getNewPassword().length() < 6) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "密码长度不能少于6位");
        }

        verifySmsCode(request.getMobile(), request.getVerifyCode());

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getMobile, request.getMobile());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "手机号未注册");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);

        String codeKey = SMS_CODE_PREFIX + request.getMobile();
        redisTemplate.delete(codeKey);

        log.info("用户重置密码成功: {}", user.getUsername());
    }

    @Override
    public User getUserById(Integer id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    @Override
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    private boolean isValidMobile(String mobile) {
        return mobile != null && mobile.matches("^1[3-9]\\d{9}$");
    }

    private void verifySmsCode(String mobile, String code) {
        if (StrUtil.isBlank(code)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "验证码不能为空");
        }

        String codeKey = SMS_CODE_PREFIX + mobile;
        String storedCode = redisTemplate.opsForValue().get(codeKey);

        if (StrUtil.isBlank(storedCode)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "验证码已过期");
        }

        if (!code.equals(storedCode)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "验证码错误");
        }

        redisTemplate.delete(codeKey);
    }

    // ── 个人中心 ────────────────────────────────────────────────────────────

    @Override
    public User getProfile(String token) {
        Integer userId = getUserIdFromToken(token);
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    @Override
    public void updateProfile(String token, UpdateProfileRequest request) {
        Integer userId = getUserIdFromToken(token);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户不存在");
        }
        if (StrUtil.isNotBlank(request.getMobile())) {
            if (!isValidMobile(request.getMobile())) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "手机号格式不正确");
            }
            user.setMobile(request.getMobile());
        }
        if (StrUtil.isNotBlank(request.getNewPassword())) {
            if (StrUtil.isBlank(request.getOldPassword())) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请输入原密码");
            }
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "原密码错误");
            }
            if (request.getNewPassword().length() < 6) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "新密码长度不能少于6位");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }
        userMapper.updateById(user);
        log.info("用户 [{}] 更新个人信息成功", user.getUsername());
    }

    // ── 管理员操作 ────────────────────────────────────────────────────────────

    @Override
    public List<User> listAllUsers() {
        List<User> users = userMapper.selectList(null);
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    @Override
    public void adminUpdateUser(Integer id, AdminUpdateUserRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户不存在");
        }
        if (StrUtil.isNotBlank(request.getUsername())) {
            user.setUsername(request.getUsername());
        }
        if (StrUtil.isNotBlank(request.getMobile())) {
            user.setMobile(request.getMobile());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (StrUtil.isNotBlank(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        userMapper.updateById(user);
        log.info("管理员更新用户 [{}] 信息成功", user.getUsername());
    }

    @Override
    public void adminDeleteUser(Integer id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户不存在");
        }
        userMapper.deleteById(id);
        log.info("管理员删除用户 [{}] 成功", user.getUsername());
    }

    // ── 工具方法 ────────────────────────────────────────────────────────────

    private Integer getUserIdFromToken(String token) {
        if (StrUtil.isBlank(token)) {
            throw new BusinessException(401, "未授权，请先登录");
        }
        String redisKey = "token:" + token;
        String userId = redisTemplate.opsForValue().get(redisKey);
        if (userId == null) {
            throw new BusinessException(401, "登录已过期，请重新登录");
        }
        return Integer.parseInt(userId);
    }
}
