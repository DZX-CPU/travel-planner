package com.bs.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bs.travel.common.BizException;
import com.bs.travel.common.Result;
import com.bs.travel.common.UserContext;
import com.bs.travel.entity.TravelPlan;
import com.bs.travel.entity.User;
import com.bs.travel.mapper.TravelPlanMapper;
import com.bs.travel.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端接口，进来先校验是不是管理员
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;
    private final TravelPlanMapper planMapper;

    private void checkAdmin() {
        User user = userMapper.selectById(UserContext.get());
        if (user == null || user.getRole() == null || user.getRole() != 1) {
            throw new BizException(403, "没有权限");
        }
    }

    @GetMapping("/user/page")
    public Result<Page<User>> userPage(@RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String keyword) {
        checkAdmin();
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<User>()
                .like(keyword != null && !keyword.isBlank(), User::getUsername, keyword)
                .orderByDesc(User::getCreateTime);
        Page<User> p = userMapper.selectPage(new Page<>(page, size), qw);
        p.getRecords().forEach(u -> u.setPassword(null));
        return Result.ok(p);
    }

    @DeleteMapping("/user/{id}")
    public Result<?> deleteUser(@PathVariable Long id) {
        checkAdmin();
        if (id.equals(UserContext.get())) {
            throw new BizException("不能删除自己");
        }
        userMapper.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        checkAdmin();
        Map<String, Object> data = new HashMap<>();
        data.put("userCount", userMapper.selectCount(null));
        data.put("planCount", planMapper.selectCount(null));

        // 各城市的行程数量，给前端画柱状图用
        List<TravelPlan> plans = planMapper.selectList(new LambdaQueryWrapper<TravelPlan>().select(TravelPlan::getCity));
        Map<String, Long> cityCount = new HashMap<>();
        for (TravelPlan p : plans) {
            cityCount.merge(p.getCity(), 1L, Long::sum);
        }
        data.put("cityStats", cityCount);
        return Result.ok(data);
    }
}
