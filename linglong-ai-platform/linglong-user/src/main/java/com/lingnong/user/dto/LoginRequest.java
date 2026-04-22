package com.lingnong.user.dto;


import lombok.Data;

import java.io.Serializable;

@Data
public class LoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;

    private String password;
}
