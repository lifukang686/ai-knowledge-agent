package com.fukang.knowledge.agent.infrastructure.config;

import com.fukang.knowledge.agent.common.context.UserContextHolder;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.application.auth.AuthSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器
 * <p>拦截所有 /api/** 请求（登录接口除外），校验请求头中的 Bearer Token 是否有效，
 * 认证通过后将用户ID存入 {@link UserContextHolder}，请求结束后自动清除</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    /** Authorization 请求头名称 */
    private static final String TOKEN_HEADER = "Authorization";
    /** Bearer Token 前缀 */
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthSessionService authSessionService;

    /**
     * 请求预处理：校验 Token 有效性
     * <p>从请求头中提取 Bearer Token，校验其格式和有效性，
     * 通过后将用户ID设置到线程上下文中</p>
     *
     * @param request  当前 HTTP 请求
     * @param response HTTP 响应
     * @param handler  目标处理器
     * @return true 表示放行，false 表示拒绝
     * @throws BaseException Token 缺失或无效时抛出 UNAUTHORIZED 或 TOKEN_INVALID
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader(TOKEN_HEADER);

        // 校验 Token 是否存在且格式正确
        if (!StringUtils.hasText(token) || !token.startsWith(BEARER_PREFIX)) {
            throw new BaseException(ErrorCodeEnum.UNAUTHORIZED);
        }

        // 提取并校验服务端会话 Token。
        String actualToken = token.substring(BEARER_PREFIX.length());
        Long userId = authSessionService.resolveUserId(actualToken);
        if (userId == null) {
            throw new BaseException(ErrorCodeEnum.TOKEN_INVALID);
        }

        // 会话有效时写入当前线程用户上下文。
        UserContextHolder.setUserId(userId);
        return true;
    }

    /**
     * 请求完成后的回调：清除线程上下文中的用户ID，防止内存泄漏
     *
     * @param request  当前 HTTP 请求
     * @param response HTTP 响应
     * @param handler  目标处理器
     * @param ex       请求处理过程中抛出的异常（可为 null）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear();
    }
}
