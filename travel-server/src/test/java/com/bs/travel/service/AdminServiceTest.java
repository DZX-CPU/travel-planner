package com.bs.travel.service;

import com.bs.travel.common.BizException;
import com.bs.travel.entity.ScenicSpot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminServiceTest {

    private ScenicSpot validSpot() {
        ScenicSpot spot = new ScenicSpot();
        spot.setName("测试景点");
        spot.setCity("杭州");
        spot.setLongitude(new BigDecimal("120.150000"));
        spot.setLatitude(new BigDecimal("30.250000"));
        return spot;
    }

    @Test
    void validSpotPasses() {
        assertDoesNotThrow(() -> AdminService.validateSpot(validSpot()));
    }

    @Test
    void nullSpotRejected() {
        assertThrows(BizException.class, () -> AdminService.validateSpot(null));
    }

    @Test
    void blankNameRejected() {
        ScenicSpot spot = validSpot();
        spot.setName("   ");
        assertThrows(BizException.class, () -> AdminService.validateSpot(spot));
    }

    @Test
    void blankCityRejected() {
        ScenicSpot spot = validSpot();
        spot.setCity("");
        assertThrows(BizException.class, () -> AdminService.validateSpot(spot));
    }

    @Test
    void missingCoordinateRejected() {
        ScenicSpot spot = validSpot();
        spot.setLongitude(null);
        assertThrows(BizException.class, () -> AdminService.validateSpot(spot));
    }

    @Test
    void longitudeOutOfRangeRejected() {
        ScenicSpot spot = validSpot();
        spot.setLongitude(new BigDecimal("181"));
        assertThrows(BizException.class, () -> AdminService.validateSpot(spot));
    }

    @Test
    void latitudeOutOfRangeRejected() {
        ScenicSpot spot = validSpot();
        spot.setLatitude(new BigDecimal("-91"));
        assertThrows(BizException.class, () -> AdminService.validateSpot(spot));
    }

    @Test
    void negativeTicketPriceRejected() {
        ScenicSpot spot = validSpot();
        spot.setTicketPrice(new BigDecimal("-1"));
        assertThrows(BizException.class, () -> AdminService.validateSpot(spot));
    }

    @Test
    void ratingOutOfRangeRejected() {
        ScenicSpot spot = validSpot();
        spot.setRating(new BigDecimal("5.1"));
        assertThrows(BizException.class, () -> AdminService.validateSpot(spot));
    }
}
