package com.kospot.kospot.domain.point.dto.response;

import com.kospot.kospot.domain.point.entity.PointHistory;
import com.kospot.kospot.domain.point.entity.PointHistoryType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PointHistoryResponse {

    private int changeAmount;
    private String description;
    private LocalDateTime changeTime;

    public static PointHistoryResponse from(PointHistory pointHistory){
        return PointHistoryResponse.builder()
                .changeAmount(pointHistory.getChangeAmount())
                .changeTime(pointHistory.getCreatedDate())
                .description(pointHistory.getPointHistoryType().getDescription())
                .build();
    }

}
