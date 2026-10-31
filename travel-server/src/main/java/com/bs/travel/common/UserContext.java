package com.bs.travel.common;

/**
 * 用 ThreadLocal 存当前登录用户id，登录校验拦截器里 set，业务代码直接 get
 */
public class UserContext {

    private static final ThreadLocal<Long> LOCAL = new ThreadLocal<>();

    public static void set(Long userId) {
        LOCAL.set(userId);
    }

    public static Long get() {
        return LOCAL.get();
    }

    // afterCompletion里必须清掉，防止线程复用串数据
    public static void clear() {
        LOCAL.remove();
    }
}
