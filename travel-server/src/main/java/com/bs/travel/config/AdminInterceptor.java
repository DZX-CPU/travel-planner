package com.bs.travel.config;

import com.bs.travel.common.BizException;
import com.bs.travel.common.UserContext;
import com.bs.travel.entity.User;
import com.bs.travel.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * /api/admin/** 统一管理员校验，避免每个接口里手写一遍。
 * 必须排在 JwtInterceptor 之后执行，UserContext 里才有当前用户。
 */
@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        Long userId = UserContext.get();
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null || user.getRole() == null || user.getRole() != 1) {
            throw new BizException(403, "没有权限");
        }
        return true;
    }
}
