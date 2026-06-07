package com.fukang.knowledge.agent.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求 DTO。
 *
 * @param username        用户名
 * @param password        密码
 * @param confirmPassword 确认密码
 */
public record RegisterReq(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 64, message = "用户名长度为3-64位")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度为6-64位")
        String password,

        @NotBlank(message = "确认密码不能为空")
        String confirmPassword
) {}
