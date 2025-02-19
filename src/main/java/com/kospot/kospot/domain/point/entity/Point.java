package com.kospot.kospot.domain.point.entity;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Embeddable
public class Point {

    private int point;

    public void add(int amount){
        this.point += amount;
    }

    public void subtract(int amount){
        if(this.point < amount){
            throw new IllegalArgumentException();
        }
        this.point -= amount;
    }

}
