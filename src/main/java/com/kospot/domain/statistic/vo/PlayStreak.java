package com.kospot.domain.statistic.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PlayStreak {
    private int currentStreak;
    private int longestStreak;
    private LocalDate lastPlayedDate;

    public static PlayStreak initialize() {
        return new PlayStreak(0, 0, null);
    }

    public void update(LocalDate playDate) {
        if (this.lastPlayedDate == null) {
            this.currentStreak = 1;
            this.longestStreak = 1;
        } else if (!playDate.equals(this.lastPlayedDate)) {
            if (playDate.equals(this.lastPlayedDate.plusDays(1))) {
                this.currentStreak++;
                if (this.currentStreak > this.longestStreak) {
                    this.longestStreak = this.currentStreak;
                }
            } else if (playDate.isAfter(this.lastPlayedDate.plusDays(1))) {
                this.currentStreak = 1;
            }
        }
        this.lastPlayedDate = playDate;
    }
}