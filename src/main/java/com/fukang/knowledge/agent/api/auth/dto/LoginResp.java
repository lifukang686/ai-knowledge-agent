package com.fukang.knowledge.agent.api.auth.dto;

/**
 * 登录响应 DTO
 *
 * @param token   认证令牌，后续请求需携带此 Token
 * @param userId  用户ID
 * @param username 用户名
 */
public record LoginResp(
        String token,
        Long userId,
        String username
) {}
