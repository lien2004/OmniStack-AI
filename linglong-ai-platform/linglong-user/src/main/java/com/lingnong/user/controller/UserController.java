package com.lingnong.user.controller;


import com.lingnong.user.common.Result;
import com.lingnong.user.dto.AdminUpdateUserRequest;
import com.lingnong.user.dto.ForgotPasswordRequest;
import com.lingnong.user.dto.LoginRequest;
import com.lingnong.user.dto.LoginResponse;
import com.lingnong.user.dto.RegisterRequest;
import com.lingnong.user.dto.SendCodeRequest;
import com.lingnong.user.dto.UpdateProfileRequest;
import com.lingnong.user.entity.User;
import com.lingnong.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<User> register(@RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return Result.success(user);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return Result.success(response);
    }

    @PostMapping("/send-code")
    public Result<String> sendVerifyCode(@RequestBody SendCodeRequest request) {
        String code = userService.sendVerifyCode(request);
        return Result.success(code);
    }

    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Integer id) {
        User user = userService.getUserById(id);
        return Result.success(user);
    }

    // ── 个人中心 ────────────────────────────────────────────────────────────

    @GetMapping("/profile")
    public Result<User> getProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractToken(authorization);
        return Result.success(userService.getProfile(token));
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody UpdateProfileRequest request) {
        String token = extractToken(authorization);
        userService.updateProfile(token, request);
        return Result.success();
    }

    // ── 管理员接口 ────────────────────────────────────────────────────────────

    @GetMapping("/admin/users")
    public Result<List<User>> listAllUsers() {
        return Result.success(userService.listAllUsers());
    }

    @PutMapping("/admin/users/{id}")
    public Result<Void> adminUpdateUser(@PathVariable Integer id,
                                         @RequestBody AdminUpdateUserRequest request) {
        userService.adminUpdateUser(id, request);
        return Result.success();
    }

    @DeleteMapping("/admin/users/{id}")
    public Result<Void> adminDeleteUser(@PathVariable Integer id) {
        userService.adminDeleteUser(id);
        return Result.success();
    }

    // ── 工具 ────────────────────────────────────────────────────────────────

    private String extractToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }
}
