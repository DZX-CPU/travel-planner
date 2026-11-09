package com.bs.travel.controller;

import com.bs.travel.common.Result;
import com.bs.travel.entity.ScenicSpot;
import com.bs.travel.gaode.GaodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "景点", description = "高德 POI 景点查询与缓存")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/spot")
@RequiredArgsConstructor
public class SpotController {

    private final GaodeService gaodeService;

    @Operation(summary = "按城市、关键词查询景点（结果缓存入库）")
    @GetMapping("/search")
    public Result<List<ScenicSpot>> search(
            @Parameter(description = "城市名，如 杭州") @RequestParam String city,
            @Parameter(description = "景点名称关键词，可选") @RequestParam(required = false) String keyword) {
        return Result.ok(gaodeService.searchSpots(city, keyword));
    }

}
