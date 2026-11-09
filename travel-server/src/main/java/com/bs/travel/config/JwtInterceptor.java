package com.bs.travel.config;

import com.bs.travel.common.JwtUtil;
import com.bs.travel.common.UserContext;
import com.bs.travel.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.bs.travel.common.BizException;

// 登录校验：除了放行名单里的路径，其他都要带token
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 浏览器预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        Long userId = token == null ? null : jwtUtil.parseToken(token);
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }

        // 管理员删号后旧token要立即失效，所以每次都确认用户在不在
        if (userMapper.selectById(userId) == null) {
            throw new BizException(401, "登录已失效，请重新登录");
        }

        UserContext.set(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
