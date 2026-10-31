package com.bs.travel.controller;

import com.bs.travel.common.Result;
import com.bs.travel.entity.ScenicSpot;
import com.bs.travel.gaode.GaodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spot")
@RequiredArgsConstructor
public class SpotController {

    private final GaodeService gaodeService;

    @GetMapping("/search")
    public Result<List<ScenicSpot>> search(@RequestParam String city,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(gaodeService.searchSpots(city, keyword));
    }

}
