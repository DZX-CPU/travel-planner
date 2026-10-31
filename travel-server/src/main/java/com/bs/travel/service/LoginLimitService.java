package com.bs.travel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 登录失败计数，防爆破。同一用户名5次失败锁15分钟。
 * Redis挂掉时这里统一放行——限流是锦上添花，不能因为它把登录搞瘫
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LoginLimitService {

    private final StringRedisTemplate redis;

    private static final String KEY_PREFIX = "login:fail:";
    private static final int MAX_FAIL = 5;
    private static final Duration LOCK_TIME = Duration.ofMinutes(15);

    public void checkLocked(String username) {
        try {
            String count = redis.opsForValue().get(KEY_PREFIX + username);
            if (count != null && Integer.parseInt(count) >= MAX_FAIL) {
                throw new com.bs.travel.common.BizException(429, "失败次数过多，请15分钟后再试");
            }
        } catch (com.bs.travel.common.BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("登录限流检查失败(Redis不可用，降级放行): {}", e.getMessage());
        }
    }

    public void recordFail(String username) {
        try {
            String key = KEY_PREFIX + username;
            Long n = redis.opsForValue().increment(key);
            if (n != null && n == 1) {
                redis.expire(key, LOCK_TIME);
            }
        } catch (Exception e) {
            log.warn("登录失败计数写入失败(Redis不可用): {}", e.getMessage());
        }
    }

    public void clear(String username) {
        try {
            redis.delete(KEY_PREFIX + username);
        } catch (Exception e) {
            log.warn("登录失败计数清理失败(Redis不可用): {}", e.getMessage());
        }
    }
}
