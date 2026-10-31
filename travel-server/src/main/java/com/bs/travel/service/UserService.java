package com.bs.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bs.travel.common.BizException;
import com.bs.travel.entity.User;
import com.bs.travel.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final com.bs.travel.service.LoginLimitService loginLimitService;

    // 简单起见用SHA-256加盐，正式项目应该上BCrypt
    public void register(String username, String password, String nickname) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count > 0) {
            throw new BizException("用户名已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(hashPassword(password));
        user.setNickname(nickname == null || nickname.isBlank() ? username : nickname);
        user.setRole(0);
        userMapper.insert(user);
    }

    public User login(String username, String password) {
        loginLimitService.checkLocked(username);
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            loginLimitService.recordFail(username);
            throw new BizException("用户名或密码错误");
        }
        if (!verifyPassword(password, user.getPassword())) {
            loginLimitService.recordFail(username);
            throw new BizException("用户名或密码错误");
        }
        loginLimitService.clear(username);
        user.setPassword(null);
        return user;
    }

    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    public void updateById(User user) {
        userMapper.updateById(user);
    }

    private String hashPassword(String password) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            String saltStr = Base64.getEncoder().encodeToString(salt);
            String digest = sha256(saltStr + password);
            return saltStr + "$" + digest;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean verifyPassword(String rawPassword, String stored) {
        String[] parts = stored.split("\\$");
        if (parts.length != 2) {
            return false;
        }
        return (parts[0] + "$" + sha256(parts[0] + rawPassword)).equals(stored);
    }

    private String sha256(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
