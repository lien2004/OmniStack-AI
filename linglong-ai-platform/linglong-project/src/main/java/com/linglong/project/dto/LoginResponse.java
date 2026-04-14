package com.linglong.project.dto;

import lombok.Data;

/**
 * 登录响应DTO
 */
@Data
public class LoginResponse {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String role;
    private String avatar;
    private String token;

    public static LoginResponse fromUser(com.linglong.project.entity.User user, String token) {
        LoginResponse response = new LoginResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setAvatar(user.getAvatar());
        response.setToken(token);
        return response;
    }
}
