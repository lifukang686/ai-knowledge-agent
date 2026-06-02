package com.fukang.knowledge.agent.application.auth.result;

/**
 * 登录成功后的认证结果。
 */
public record LoginResult(
        String token,
        Long userId,
        String username
) {}
