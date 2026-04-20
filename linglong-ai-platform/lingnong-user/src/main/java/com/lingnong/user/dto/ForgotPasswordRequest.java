package com.lingnong.user.dto;


import lombok.Data;

import java.io.Serializable;

@Data
public class ForgotPasswordRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String mobile;

    private String verifyCode;

    private String newPassword;
}
