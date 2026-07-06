package com.fukang.knowledge.agent.module.auth.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注册命令。
 *
 * @param username        用户名
 * @param password        密码
 * @param confirmPassword 确认密码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterCommand {
    private String username;

    private String password;

    private String confirmPassword;

}
