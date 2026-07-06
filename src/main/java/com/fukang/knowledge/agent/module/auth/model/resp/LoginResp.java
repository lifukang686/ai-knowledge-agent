package com.fukang.knowledge.agent.module.auth.model.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 DTO
 *
 * @param token   认证令牌，后续请求需携带此 Token
 * @param userId  用户ID
 * @param username 用户名
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResp {
    private String token;

    private Long userId;

    private String username;

}
