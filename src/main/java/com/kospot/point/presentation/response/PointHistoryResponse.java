package com.kospot.point.presentation.response;

import com.kospot.point.domain.entity.PointHistory;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
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
