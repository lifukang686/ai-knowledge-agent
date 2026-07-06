package com.fukang.knowledge.agent.module.auth.service;

import com.fukang.knowledge.agent.module.auth.model.dto.LoginCommand;
import com.fukang.knowledge.agent.module.auth.model.dto.RegisterCommand;
import com.fukang.knowledge.agent.module.auth.mapper.UserMapper;
import com.fukang.knowledge.agent.module.auth.model.vo.LoginResult;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.module.auth.model.entity.UserEntity;
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
public class AuthService {

    private final UserMapper userMapper;
    private final AuthSessionService authSessionService;

    /**
     * 用户登录
     * <p>根据用户名查询用户，校验密码后颁发服务端会话 Token。
     * 当前为 MVP 阶段，密码采用明文比对。</p>
     *
     * @param command 登录命令，包含用户名和密码
     * @return 登录响应，包含 Token、用户ID和用户名
     * @throws BaseException 用户不存在时抛出 USER_NOT_EXIST，密码错误时抛出 PASSWORD_ERROR
     */
    public LoginResult login(LoginCommand command) {
        // 根据用户名查询用户
        UserEntity user = userMapper.findByUsername(command.getUsername());

        if (user == null) {
            throw new BaseException(ErrorCodeEnum.USER_NOT_EXIST);
        }

        // 极简密码校验：明文比对，未引入加密库
        if (!command.getPassword().equals(user.getPasswordHash())) {
            throw new BaseException(ErrorCodeEnum.PASSWORD_ERROR);
        }

        // 颁发随机会话 Token，后续请求只信任服务端会话映射。
        String token = authSessionService.createSession(user.getId());
        log.info("UserEntity {} logged in successfully.", user.getUsername());
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
        String username = normalizeUsername(command.getUsername());
        if (!command.getPassword().equals(command.getConfirmPassword())) {
            throw new BaseException(ErrorCodeEnum.PASSWORD_CONFIRM_NOT_MATCH);
        }
        if (userMapper.findByUsername(username) != null) {
            throw new BaseException(ErrorCodeEnum.USER_ALREADY_EXISTS);
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPasswordHash(command.getPassword());
        userMapper.insert(user);

        log.info("UserEntity {} registered successfully.", username);
        return new LoginResult(authSessionService.createSession(user.getId()), user.getId(), user.getUsername());
    }

    private String normalizeUsername(String username) {
        return StringUtils.hasText(username) ? username.trim() : "";
    }

}
