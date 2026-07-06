package com.fukang.knowledge.agent.module.auth.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录认证命令。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginCommand {
    private String username;

    private String password;

}
