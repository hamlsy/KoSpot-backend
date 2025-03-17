package com.kospot.kospot.presentation.point.dto.response;

import com.kospot.kospot.domain.point.entity.PointHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PointHistoryResponse {

    private int changeAmount;
    private String description;
    private LocalDateTime createdDate;

    public static PointHistoryResponse from(PointHistory pointHistory){
        return PointHistoryResponse.builder()
                .changeAmount(pointHistory.getChangeAmount())
                .createdDate(pointHistory.getCreatedDate())
                .description(pointHistory.getPointHistoryType().getDescription())
                .build();
    }

}
