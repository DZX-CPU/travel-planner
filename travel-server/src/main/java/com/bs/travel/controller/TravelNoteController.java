package com.bs.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bs.travel.common.BizException;
import com.bs.travel.common.Result;
import com.bs.travel.common.UserContext;
import com.bs.travel.entity.TravelNote;
import com.bs.travel.mapper.TravelNoteMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Tag(name = "游记", description = "游记社区：列表、关键词搜索、详情、发布、删除")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/note")
@RequiredArgsConstructor
public class TravelNoteController {

    private final TravelNoteMapper noteMapper;

    /**
     * 游记列表，带分页，可以按城市或关键词筛选
     */
    @Operation(summary = "游记列表：分页，可按城市、标题/正文关键词筛选")
    @GetMapping("/list")
    public Result<Page<TravelNote>> list(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long size,
            @Parameter(description = "城市，可选") @RequestParam(required = false) String city,
            @Parameter(description = "标题或正文关键词，可选") @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<TravelNote> qw = new LambdaQueryWrapper<TravelNote>()
                .eq(StringUtils.hasText(city), TravelNote::getCity, city)
                .and(StringUtils.hasText(keyword), w -> w.like(TravelNote::getTitle, keyword)
                        .or().like(TravelNote::getContent, keyword))
                .orderByDesc(TravelNote::getCreateTime);
        return Result.ok(noteMapper.selectPage(new Page<>(page, size), qw));
    }

    @Operation(summary = "发布游记")
    @PostMapping
    public Result<?> publish(@RequestBody TravelNote note) {
        if (!StringUtils.hasText(note.getTitle()) || !StringUtils.hasText(note.getContent())) {
            throw new BizException("标题和内容不能为空");
        }
        note.setUserId(UserContext.get());
        note.setViewCount(0);
        noteMapper.insert(note);
        return Result.ok();
    }

    @Operation(summary = "游记详情（浏览量 +1）")
    @GetMapping("/{id}")
    public Result<TravelNote> detail(@PathVariable Long id) {
        TravelNote note = noteMapper.selectById(id);
        if (note == null) {
            throw new BizException("游记不存在");
        }
        // 浏览量+1
        TravelNote update = new TravelNote();
        update.setId(id);
        update.setViewCount(note.getViewCount() + 1);
        noteMapper.updateById(update);
        return Result.ok(note);
    }

    @Operation(summary = "删除自己的游记")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        TravelNote note = noteMapper.selectById(id);
        if (note == null || !note.getUserId().equals(UserContext.get())) {
            throw new BizException("游记不存在");
        }
        noteMapper.deleteById(id);
        return Result.ok();
    }

    @Operation(summary = "我的游记")
    @GetMapping("/my")
    public Result<Page<TravelNote>> my(@RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "10") long size) {
        LambdaQueryWrapper<TravelNote> qw = new LambdaQueryWrapper<TravelNote>()
                .eq(TravelNote::getUserId, UserContext.get())
                .orderByDesc(TravelNote::getCreateTime);
        return Result.ok(noteMapper.selectPage(new Page<>(page, size), qw));
    }
}
