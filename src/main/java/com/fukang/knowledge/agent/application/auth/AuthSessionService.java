package com.fukang.knowledge.agent.application.auth;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证会话服务。
 * <p>使用服务端随机会话 Token，避免客户端伪造用户 ID。</p>
 */
@Service
public class AuthSessionService {

    /** 会话 Token 字节长度。 */
    private static final int TOKEN_BYTES = 32;
    /** 会话有效期。 */
    private static final Duration SESSION_TTL = Duration.ofHours(12);

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /**
     * 创建登录会话。
     */
    public String createSession(Long userId) {
        String token = generateToken();
        sessions.put(token, new Session(userId, Instant.now().plus(SESSION_TTL)));
        return token;
    }

    /**
     * 根据 Token 解析用户 ID。
     */
    public Long resolveUserId(String token) {
        Session session = sessions.get(token);
        if (session == null) {
            return null;
        }
        if (session.expireAt().isBefore(Instant.now())) {
            sessions.remove(token);
            return null;
        }
        return session.userId();
    }

    /**
     * 生成不可预测的会话 Token。
     */
    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 登录会话快照。
     */
    private record Session(Long userId, Instant expireAt) {
    }
}
