package com.fukang.knowledge.agent.module.auth.application.command;

/**
 * 登录认证命令。
 */
public record LoginCommand(
        String username,
        String password
) {}
