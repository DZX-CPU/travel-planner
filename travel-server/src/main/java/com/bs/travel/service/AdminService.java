package com.bs.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bs.travel.common.BizException;
import com.bs.travel.entity.ScenicSpot;
import com.bs.travel.entity.TravelNote;
import com.bs.travel.entity.TravelPlan;
import com.bs.travel.entity.User;
import com.bs.travel.mapper.PlanItemMapper;
import com.bs.travel.mapper.ScenicSpotMapper;
import com.bs.travel.mapper.TravelNoteMapper;
import com.bs.travel.mapper.TravelPlanMapper;
import com.bs.travel.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 后台管理：用户、景点、行程、游记、运营统计。
 * 管理员身份由 AdminInterceptor 统一校验，这里只做业务规则。
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final ScenicSpotMapper spotMapper;
    private final TravelPlanMapper planMapper;
    private final TravelNoteMapper noteMapper;
    private final PlanItemMapper itemMapper;
    private final TravelPlanService planService;

    /* ---------------- 用户 ---------------- */

    public Page<User> userPage(long page, long size, String keyword) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<User>()
                .like(StringUtils.hasText(keyword), User::getUsername, keyword)
                .orderByDesc(User::getCreateTime);
        Page<User> p = userMapper.selectPage(new Page<>(page, size), qw);
        p.getRecords().forEach(u -> u.setPassword(null));
        return p;
    }

    public void deleteUser(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new BizException("不能删除自己");
        }
        if (userMapper.selectById(id) == null) {
            throw new BizException("用户不存在");
        }
        userMapper.deleteById(id);
    }

    /* ---------------- 景点 ---------------- */

    public Page<ScenicSpot> spotPage(long page, long size, String city, String keyword) {
        LambdaQueryWrapper<ScenicSpot> qw = new LambdaQueryWrapper<ScenicSpot>()
                .eq(StringUtils.hasText(city), ScenicSpot::getCity, city)
                .and(StringUtils.hasText(keyword), w -> w.like(ScenicSpot::getName, keyword)
                        .or().like(ScenicSpot::getAddress, keyword))
                .orderByDesc(ScenicSpot::getId);
        return spotMapper.selectPage(new Page<>(page, size), qw);
    }

    @Transactional
    public ScenicSpot createSpot(ScenicSpot form) {
        validateSpot(form);
        ScenicSpot spot = new ScenicSpot();
        copyEditable(spot, form);
        spotMapper.insert(spot);
        return spot;
    }

    @Transactional
    public void updateSpot(Long id, ScenicSpot form) {
        ScenicSpot spot = spotMapper.selectById(id);
        if (spot == null) {
            throw new BizException("景点不存在");
        }
        validateSpot(form);
        copyEditable(spot, form);
        spotMapper.updateById(spot);
    }

    @Transactional
    public void deleteSpot(Long id) {
        ScenicSpot spot = spotMapper.selectById(id);
        if (spot == null) {
            throw new BizException("景点不存在");
        }
        // 清掉高德ID再逻辑删。否则唯一键 uk_gaode_id 还会占着这个POI，
        // 以后用户再查同一城市时缓存不进库，景点会一直消失
        if (spot.getGaodeId() != null) {
            spotMapper.update(null, new LambdaUpdateWrapper<ScenicSpot>()
                    .eq(ScenicSpot::getId, id)
                    .setSql("gaode_id = NULL"));
        }
        spotMapper.deleteById(id);
    }

    /* ---------------- 行程 ---------------- */

    public Page<TravelPlan> planPage(long page, long size, String keyword) {
        LambdaQueryWrapper<TravelPlan> qw = new LambdaQueryWrapper<TravelPlan>()
                .and(StringUtils.hasText(keyword), w -> w.like(TravelPlan::getTitle, keyword)
                        .or().like(TravelPlan::getCity, keyword))
                .orderByDesc(TravelPlan::getCreateTime);
        Page<TravelPlan> p = planMapper.selectPage(new Page<>(page, size), qw);
        fillUsernames(p.getRecords(), TravelPlan::getUserId, TravelPlan::setUsername);
        return p;
    }

    @Transactional
    public void deletePlan(Long id) {
        planService.deletePlanForAdmin(id);
    }

    /* ---------------- 游记 ---------------- */

    public Page<TravelNote> notePage(long page, long size, String keyword, String city) {
        LambdaQueryWrapper<TravelNote> qw = new LambdaQueryWrapper<TravelNote>()
                .eq(StringUtils.hasText(city), TravelNote::getCity, city)
                .and(StringUtils.hasText(keyword), w -> w.like(TravelNote::getTitle, keyword)
                        .or().like(TravelNote::getContent, keyword))
                .orderByDesc(TravelNote::getCreateTime);
        Page<TravelNote> p = noteMapper.selectPage(new Page<>(page, size), qw);
        fillUsernames(p.getRecords(), TravelNote::getUserId, TravelNote::setUsername);
        return p;
    }

    @Transactional
    public void deleteNote(Long id) {
        if (noteMapper.selectById(id) == null) {
            throw new BizException("游记不存在");
        }
        noteMapper.deleteById(id);
    }

    /* ---------------- 统计 ---------------- */

    public Map<String, Object> stats() {
        Map<String, Object> data = new HashMap<>();
        data.put("userCount", userMapper.selectCount(null));
        data.put("planCount", planMapper.selectCount(null));
        data.put("noteCount", noteMapper.selectCount(null));

        // 各城市的行程数量，给前端画柱状图用
        List<TravelPlan> plans = planMapper.selectList(new LambdaQueryWrapper<TravelPlan>()
                .select(TravelPlan::getCity));
        Map<String, Long> cityStats = new HashMap<>();
        for (TravelPlan p : plans) {
            cityStats.merge(p.getCity(), 1L, Long::sum);
        }
        data.put("cityStats", cityStats);
        data.put("hotSpots", hotSpots());
        return data;
    }

    private List<HotSpotStat> hotSpots() {
        return itemMapper.selectHotSpots().stream()
                .map(row -> new HotSpotStat(
                        String.valueOf(row.get("name")),
                        ((Number) row.get("cnt")).longValue(),
                        row.get("city") == null ? null : String.valueOf(row.get("city"))))
                .toList();
    }

    // 后台列表要显示作者名，按 userId 批量回填，避免N+1
    private <T> void fillUsernames(List<T> records, Function<T, Long> idGetter,
                                   BiConsumer<T, String> nameSetter) {
        Set<Long> ids = records.stream().map(idGetter).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> names = userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
        for (T record : records) {
            nameSetter.accept(record, names.get(idGetter.apply(record)));
        }
    }

    /* ---------------- 景点校验 ---------------- */

    static void validateSpot(ScenicSpot spot) {
        if (spot == null) {
            throw new BizException("景点信息不能为空");
        }
        requireText(spot.getName(), "景点名称不能为空", 100);
        requireText(spot.getCity(), "城市不能为空", 50);
        if (spot.getLongitude() == null || spot.getLatitude() == null) {
            throw new BizException("经度和纬度不能为空");
        }
        if (spot.getLongitude().compareTo(new BigDecimal("-180")) < 0
                || spot.getLongitude().compareTo(new BigDecimal("180")) > 0) {
            throw new BizException("经度需在 -180 到 180 之间");
        }
        if (spot.getLatitude().compareTo(new BigDecimal("-90")) < 0
                || spot.getLatitude().compareTo(new BigDecimal("90")) > 0) {
            throw new BizException("纬度需在 -90 到 90 之间");
        }
        if (spot.getTicketPrice() != null && spot.getTicketPrice().signum() < 0) {
            throw new BizException("门票价格不能为负数");
        }
        if (spot.getRating() != null
                && (spot.getRating().compareTo(BigDecimal.ZERO) < 0
                || spot.getRating().compareTo(new BigDecimal("5")) > 0)) {
            throw new BizException("评分需在 0 到 5 之间");
        }
        if (spot.getAddress() != null && spot.getAddress().length() > 255) {
            throw new BizException("地址不能超过255个字符");
        }
        if (spot.getType() != null && spot.getType().length() > 50) {
            throw new BizException("类型不能超过50个字符");
        }
        if (spot.getImageUrl() != null && spot.getImageUrl().length() > 255) {
            throw new BizException("图片地址不能超过255个字符");
        }
    }

    private static void requireText(String value, String blankMessage, int maxLength) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(blankMessage);
        }
        if (value.length() > maxLength) {
            throw new BizException(blankMessage.replace("不能为空", "不能超过" + maxLength + "个字符"));
        }
    }

    private static void copyEditable(ScenicSpot target, ScenicSpot form) {
        target.setName(form.getName().trim());
        target.setCity(form.getCity().trim());
        target.setType(form.getType());
        target.setAddress(form.getAddress());
        target.setLongitude(form.getLongitude());
        target.setLatitude(form.getLatitude());
        target.setTicketPrice(form.getTicketPrice());
        target.setRating(form.getRating());
        target.setImageUrl(form.getImageUrl());
        target.setDescription(form.getDescription());
    }

    /**
     * 热门景点统计项，Jackson 直接序列化成 {name,count,city}
     */
    public record HotSpotStat(String name, long count, String city) {
    }
}
