package com.fukang.knowledge.agent.application.auth.command;

public record LoginCommand(
        String username,
        String password
) {}
