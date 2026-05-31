package com.fukang.knowledge.agent.application.auth.result;

public record LoginResult(
        String token,
        Long userId,
        String username
) {}
