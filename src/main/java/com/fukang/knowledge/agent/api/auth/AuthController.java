package com.fukang.knowledge.agent.api.auth;

import com.fukang.knowledge.agent.api.auth.dto.LoginReq;
import com.fukang.knowledge.agent.api.auth.dto.LoginResp;
import com.fukang.knowledge.agent.api.auth.dto.RegisterReq;
import com.fukang.knowledge.agent.application.auth.AuthAppService;
import com.fukang.knowledge.agent.application.auth.command.LoginCommand;
import com.fukang.knowledge.agent.application.auth.command.RegisterCommand;
import com.fukang.knowledge.agent.application.auth.result.LoginResult;
import com.fukang.knowledge.agent.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 * <p>提供用户登录认证相关接口，登录接口不受 AuthInterceptor 拦截</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthAppService authAppService;

    /**
     * 用户登录
     *
     * @param req 登录请求参数，包含用户名和密码
     * @return 登录成功后返回 Token、用户ID和用户名
     */
    @PostMapping("/login")
    public Result<LoginResp> login(@RequestBody @Validated LoginReq req) {
        LoginResult result = authAppService.login(new LoginCommand(req.username(), req.password()));
        return Result.success(new LoginResp(result.token(), result.userId(), result.username()));
    }

    /**
     * 用户注册。
     *
     * @param req 注册请求参数
     * @return 注册成功后返回 Token、用户ID和用户名
     */
    @PostMapping("/register")
    public Result<LoginResp> register(@RequestBody @Validated RegisterReq req) {
        LoginResult result = authAppService.register(
                new RegisterCommand(req.username(), req.password(), req.confirmPassword()));
        return Result.success(new LoginResp(result.token(), result.userId(), result.username()));
    }
}
