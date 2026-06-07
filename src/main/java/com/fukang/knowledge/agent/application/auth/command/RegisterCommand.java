package com.fukang.knowledge.agent.application.auth.command;

/**
 * 注册命令。
 *
 * @param username        用户名
 * @param password        密码
 * @param confirmPassword 确认密码
 */
public record RegisterCommand(
        String username,
        String password,
        String confirmPassword
) {}
