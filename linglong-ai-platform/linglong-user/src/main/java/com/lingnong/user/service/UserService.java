package com.lingnong.user.service;

import com.lingnong.user.dto.AdminUpdateUserRequest;
import com.lingnong.user.dto.ForgotPasswordRequest;
import com.lingnong.user.dto.LoginRequest;
import com.lingnong.user.dto.LoginResponse;
import com.lingnong.user.dto.RegisterRequest;
import com.lingnong.user.dto.SendCodeRequest;
import com.lingnong.user.dto.UpdateProfileRequest;
import com.lingnong.user.entity.User;
import java.util.List;

public interface UserService {

    User register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    String sendVerifyCode(SendCodeRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    User getUserById(Integer id);

    User getUserByUsername(String username);

    // 个人中心
    User getProfile(String token);

    void updateProfile(String token, UpdateProfileRequest request);

    // 管理员操作
    List<User> listAllUsers();

    void adminUpdateUser(Integer id, AdminUpdateUserRequest request);

    void adminDeleteUser(Integer id);
}

