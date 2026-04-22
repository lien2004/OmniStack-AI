package com.lingnong.user.dto;


import lombok.Data;

import java.io.Serializable;

@Data
public class RegisterRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;

    private String password;

    private String mobile;

    private String verifyCode;

    /** 角色：0-普通用户，1-系统管理员，默认0 */
    private Integer role;
}
