package com.bs.travel.controller;

import com.bs.travel.common.BizException;
import com.bs.travel.common.JwtUtil;
import com.bs.travel.common.Result;
import com.bs.travel.common.UserContext;
import com.bs.travel.entity.User;
import com.bs.travel.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "用户", description = "注册、登录、个人信息")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Data
    public static class LoginDTO {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class RegisterDTO {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
        private String nickname;
    }

    @Operation(summary = "普通用户注册")
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto.getUsername(), dto.getPassword(), dto.getNickname());
        return Result.ok();
    }

    @Operation(summary = "登录，返回 JWT 与用户信息")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        User user = userService.login(dto.getUsername(), dto.getPassword());
        String token = jwtUtil.generateToken(user.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        return Result.ok(data);
    }

    @Operation(summary = "查询当前登录用户信息")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/info")
    public Result<User> info() {
        User user = userService.getById(UserContext.get());
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return Result.ok(user);
    }

    @Operation(summary = "修改当前用户的昵称、头像、邮箱、手机号")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/info")
    public Result<?> updateInfo(@RequestBody User user) {
        // 只允许改这几个字段，防止越权改role
        User u = new User();
        u.setId(UserContext.get());
        u.setNickname(user.getNickname());
        u.setAvatar(user.getAvatar());
        u.setEmail(user.getEmail());
        u.setPhone(user.getPhone());
        userService.updateById(u);
        return Result.ok();
    }
}
