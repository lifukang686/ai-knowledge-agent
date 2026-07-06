package com.fukang.knowledge.agent.module.auth.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功后的认证结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResult {
    private String token;

    private Long userId;

    private String username;

}
