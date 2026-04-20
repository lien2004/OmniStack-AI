package com.lingnong.user.dto;

import com.lingnong.user.entity.User;
import lombok.Data;

import java.io.Serializable;

@Data
public class LoginResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private User user;
    private String token;
}
