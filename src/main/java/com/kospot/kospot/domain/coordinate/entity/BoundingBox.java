package com.kospot.kospot.domain.coordinate.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BoundingBox {
    private final double minLat;
    private final double maxLat;
    private final double minLng;
    private final double maxLng;
}
