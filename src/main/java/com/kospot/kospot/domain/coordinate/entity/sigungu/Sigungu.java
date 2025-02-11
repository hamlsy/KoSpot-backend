package com.kospot.kospot.domain.coordinate.entity.sigungu;

import com.kospot.kospot.domain.coordinate.entity.sido.Sido;

public interface Sigungu {
    Sido getParentSido();
    String getName();

    static <T extends Enum<T> & Sigungu> T fromName(Class<T> classType, String name) {
        for (T sigungu : classType.getEnumConstants()) {
            if (sigungu.getName().equals(name)) {
                return sigungu;
            }
        }
        throw new IllegalArgumentException("No enum constant with name " + name);
    }

}
