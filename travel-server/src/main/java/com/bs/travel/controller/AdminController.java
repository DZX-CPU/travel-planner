package com.bs.travel.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bs.travel.common.Result;
import com.bs.travel.common.UserContext;
import com.bs.travel.entity.ScenicSpot;
import com.bs.travel.entity.TravelNote;
import com.bs.travel.entity.TravelPlan;
import com.bs.travel.entity.User;
import com.bs.travel.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理端接口。管理员身份由 AdminInterceptor 统一校验（/api/admin/**）。
 */
@Tag(name = "后台管理", description = "管理员专用：用户、景点、行程、游记、运营统计")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /* ---------------- 用户 ---------------- */

    @Operation(summary = "用户分页，可按用户名搜索")
    @GetMapping("/user/page")
    public Result<Page<User>> userPage(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long size,
            @Parameter(description = "用户名关键词，可选") @RequestParam(required = false) String keyword) {
        return Result.ok(adminService.userPage(page, size, keyword));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/user/{id}")
    public Result<?> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id, UserContext.get());
        return Result.ok();
    }

    /* ---------------- 统计 ---------------- */

    @Operation(summary = "运营统计：用户规模、行程生成量、游记量、城市分布、热门景点TOP10")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(adminService.stats());
    }

    /* ---------------- 景点 ---------------- */

    @Operation(summary = "景点分页，可按城市、名称/地址关键词筛选")
    @GetMapping("/spot/page")
    public Result<Page<ScenicSpot>> spotPage(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @Parameter(description = "城市，可选") @RequestParam(required = false) String city,
            @Parameter(description = "名称或地址关键词，可选") @RequestParam(required = false) String keyword) {
        return Result.ok(adminService.spotPage(page, size, city, keyword));
    }

    @Operation(summary = "新增景点（管理员维护景点库）")
    @PostMapping("/spot")
    public Result<ScenicSpot> createSpot(@RequestBody ScenicSpot spot) {
        return Result.ok(adminService.createSpot(spot));
    }

    @Operation(summary = "修改景点")
    @PutMapping("/spot/{id}")
    public Result<?> updateSpot(@PathVariable Long id, @RequestBody ScenicSpot spot) {
        adminService.updateSpot(id, spot);
        return Result.ok();
    }

    @Operation(summary = "删除景点")
    @DeleteMapping("/spot/{id}")
    public Result<?> deleteSpot(@PathVariable Long id) {
        adminService.deleteSpot(id);
        return Result.ok();
    }

    /* ---------------- 行程 ---------------- */

    @Operation(summary = "全站行程分页，可按标题/城市关键词搜索")
    @GetMapping("/plan/page")
    public Result<Page<TravelPlan>> planPage(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @Parameter(description = "标题或城市关键词，可选") @RequestParam(required = false) String keyword) {
        return Result.ok(adminService.planPage(page, size, keyword));
    }

    @Operation(summary = "删除全站任意行程（同时清理天和项）")
    @DeleteMapping("/plan/{id}")
    public Result<?> deletePlan(@PathVariable Long id) {
        adminService.deletePlan(id);
        return Result.ok();
    }

    /* ---------------- 游记 ---------------- */

    @Operation(summary = "全站游记分页，可按标题/正文关键词、城市筛选")
    @GetMapping("/note/page")
    public Result<Page<TravelNote>> notePage(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @Parameter(description = "标题或正文关键词，可选") @RequestParam(required = false) String keyword,
            @Parameter(description = "城市，可选") @RequestParam(required = false) String city) {
        return Result.ok(adminService.notePage(page, size, keyword, city));
    }

    @Operation(summary = "删除全站任意游记")
    @DeleteMapping("/note/{id}")
    public Result<?> deleteNote(@PathVariable Long id) {
        adminService.deleteNote(id);
        return Result.ok();
    }
}
