package com.kospot.kospot.domain.coordinate.dto.response.kakao;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoPanoResponse {
    private String panoId;
    private double latitude;
    private double longitude;
}
