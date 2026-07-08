package com.fukang.knowledge.agent.module.auth.model.resp;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResp {
    /** 认证令牌，后续请求需携带此 Token */
    private String token;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

}
