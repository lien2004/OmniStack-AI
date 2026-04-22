package com.lingnong.user.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class AdminUpdateUserRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;

    private String mobile;

    /** 角色：0-普通用户，1-系统管理员 */
    private Integer role;

    /** 新密码（可选，不传则不修改） */
    private String password;
}
