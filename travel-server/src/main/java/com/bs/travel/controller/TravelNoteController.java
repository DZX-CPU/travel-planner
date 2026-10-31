package com.bs.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bs.travel.common.BizException;
import com.bs.travel.common.Result;
import com.bs.travel.common.UserContext;
import com.bs.travel.entity.TravelNote;
import com.bs.travel.mapper.TravelNoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/api/note")
@RequiredArgsConstructor
public class TravelNoteController {

    private final TravelNoteMapper noteMapper;

    /**
     * 游记列表，带分页，可以按城市筛选
     */
    @GetMapping("/list")
    public Result<Page<TravelNote>> list(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String city) {
        LambdaQueryWrapper<TravelNote> qw = new LambdaQueryWrapper<TravelNote>()
                .eq(StringUtils.hasText(city), TravelNote::getCity, city)
                .orderByDesc(TravelNote::getCreateTime);
        return Result.ok(noteMapper.selectPage(new Page<>(page, size), qw));
    }

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

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        TravelNote note = noteMapper.selectById(id);
        if (note == null || !note.getUserId().equals(UserContext.get())) {
            throw new BizException("游记不存在");
        }
        noteMapper.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/my")
    public Result<Page<TravelNote>> my(@RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "10") long size) {
        LambdaQueryWrapper<TravelNote> qw = new LambdaQueryWrapper<TravelNote>()
                .eq(TravelNote::getUserId, UserContext.get())
                .orderByDesc(TravelNote::getCreateTime);
        return Result.ok(noteMapper.selectPage(new Page<>(page, size), qw));
    }
}
