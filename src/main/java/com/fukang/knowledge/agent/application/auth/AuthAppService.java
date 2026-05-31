package com.fukang.knowledge.agent.application.auth;

import com.fukang.knowledge.agent.application.auth.command.LoginCommand;
import com.fukang.knowledge.agent.application.auth.port.UserRepository;
import com.fukang.knowledge.agent.application.auth.result.LoginResult;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.UserDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 认证应用服务
 * <p>处理用户登录认证逻辑，包括用户查询、密码校验和 Token 颁发</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAppService {

    private final UserRepository userRepository;

    /**
     * 用户登录
     * <p>根据用户名查询用户，校验密码后颁发 Mock Token。
     * 当前为 MVP 阶段，密码采用明文比对，Token 为固定 Mock 值。</p>
     *
     * @param command 登录命令，包含用户名和密码
     * @return 登录响应，包含 Token、用户ID和用户名
     * @throws BaseException 用户不存在时抛出 USER_NOT_EXIST，密码错误时抛出 PASSWORD_ERROR
     */
    public LoginResult login(LoginCommand command) {
        // 根据用户名查询用户
        UserDO user = userRepository.findByUsername(command.username());

        if (user == null) {
            // MVP 阶段：当数据库无数据时，允许 admin/admin123 作为后门登录
            if ("admin".equals(command.username()) && "admin123".equals(command.password())) {
                log.info("Mock admin login success.");
                return new LoginResult("mock-token-123456", 1L, "admin");
            }
            throw new BaseException(ErrorCodeEnum.USER_NOT_EXIST);
        }

        // 极简密码校验：明文比对，未引入加密库
        if (!command.password().equals(user.getPasswordHash())) {
            throw new BaseException(ErrorCodeEnum.PASSWORD_ERROR);
        }

        // 颁发 Mock Token（后续替换为 JWT）
        String token = "mock-token-123456";
        log.info("User {} logged in successfully.", user.getUsername());
        return new LoginResult(token, user.getId(), user.getUsername());
    }
}
