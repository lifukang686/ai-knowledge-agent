package com.fukang.knowledge.agent.common.context;

/**
 * 用户上下文持有者
 * <p>基于 ThreadLocal 在当前线程中存储和获取已认证用户的ID，
 * 由 {@link com.fukang.knowledge.agent.infrastructure.config.AuthInterceptor} 在请求进入时设置，
 * 请求结束后清除，避免内存泄漏</p>
 */
public class UserContextHolder {

    /** 存储当前线程用户ID的 ThreadLocal */
    private static final ThreadLocal<Long> USER_ID_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置当前线程的用户ID
     *
     * @param userId 用户ID
     */
    public static void setUserId(Long userId) {
        USER_ID_THREAD_LOCAL.set(userId);
    }

    /**
     * 获取当前线程的用户ID
     *
     * @return 当前线程的用户ID，未设置时返回 null
     */
    public static Long getUserId() {
        return USER_ID_THREAD_LOCAL.get();
    }

    /**
     * 清除当前线程的用户ID，防止内存泄漏
     * <p>必须在请求结束后调用，通常在 Interceptor 的 afterCompletion 中执行</p>
     */
    public static void clear() {
        USER_ID_THREAD_LOCAL.remove();
    }
}
