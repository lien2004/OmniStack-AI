package com.lingnong.user.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class UpdateProfileRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String mobile;

    private String oldPassword;

    private String newPassword;
}
