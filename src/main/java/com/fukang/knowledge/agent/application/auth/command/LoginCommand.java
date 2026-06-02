package com.fukang.knowledge.agent.application.auth.command;

/**
 * 登录认证命令。
 */
public record LoginCommand(
        String username,
        String password
) {}
