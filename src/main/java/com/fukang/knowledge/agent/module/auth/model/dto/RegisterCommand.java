package com.fukang.knowledge.agent.module.auth.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注册命令。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterCommand {
    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /** 确认密码 */
    private String confirmPassword;

}
