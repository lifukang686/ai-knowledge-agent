package com.fukang.knowledge.agent.application.auth;

import com.fukang.knowledge.agent.application.auth.command.LoginCommand;
import com.fukang.knowledge.agent.application.auth.command.RegisterCommand;
import com.fukang.knowledge.agent.application.auth.port.UserRepository;
import com.fukang.knowledge.agent.application.auth.result.LoginResult;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.UserDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 认证应用服务
 * <p>处理用户登录认证逻辑，包括用户查询、密码校验和 Token 颁发。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAppService {

    private final UserRepository userRepository;

    /**
     * 用户登录
     * <p>根据用户名查询用户，校验密码后颁发 Mock Token。
     * 当前为 MVP 阶段，密码采用明文比对。</p>
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
                return new LoginResult(buildMockToken(1L), 1L, "admin");
            }
            throw new BaseException(ErrorCodeEnum.USER_NOT_EXIST);
        }

        // 极简密码校验：明文比对，未引入加密库
        if (!command.password().equals(user.getPasswordHash())) {
            throw new BaseException(ErrorCodeEnum.PASSWORD_ERROR);
        }

        // 颁发携带用户ID的 Mock Token，后续替换为 JWT。
        String token = buildMockToken(user.getId());
        log.info("User {} logged in successfully.", user.getUsername());
        return new LoginResult(token, user.getId(), user.getUsername());
    }

    /**
     * 用户注册。
     * <p>MVP 阶段沿用明文密码存储，后续统一替换为加密存储。</p>
     *
     * @param command 注册命令
     * @return 注册后的登录态
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginResult register(RegisterCommand command) {
        String username = normalizeUsername(command.username());
        if (!command.password().equals(command.confirmPassword())) {
            throw new BaseException(ErrorCodeEnum.PASSWORD_CONFIRM_NOT_MATCH);
        }
        if (userRepository.findByUsername(username) != null) {
            throw new BaseException(ErrorCodeEnum.USER_ALREADY_EXISTS);
        }

        UserDO user = new UserDO();
        user.setUsername(username);
        user.setPasswordHash(command.password());
        userRepository.insert(user);

        log.info("User {} registered successfully.", username);
        return new LoginResult(buildMockToken(user.getId()), user.getId(), user.getUsername());
    }

    private String normalizeUsername(String username) {
        return StringUtils.hasText(username) ? username.trim() : "";
    }

    private String buildMockToken(Long userId) {
        return "mock-token-" + userId;
    }
}
