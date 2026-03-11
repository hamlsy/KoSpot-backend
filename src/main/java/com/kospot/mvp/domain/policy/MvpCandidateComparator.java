package com.kospot.mvp.domain.policy;

import com.kospot.mvp.domain.vo.MvpCandidateSnapshot;
import org.springframework.stereotype.Component;

@Component
public class MvpCandidateComparator {

    public boolean isBetter(MvpCandidateSnapshot candidate, MvpCandidateSnapshot current) {
        candidate.validate();
        if (current == null) {
            return true;
        }
        current.validate();

        int scoreCompare = Double.compare(candidate.score(), current.score());
        if (scoreCompare != 0) {
            return scoreCompare > 0;
        }

        int endedAtCompare = candidate.endedAt().compareTo(current.endedAt());
        if (endedAtCompare != 0) {
            return endedAtCompare < 0;
        }

        return candidate.roadViewGameId() < current.roadViewGameId();
    }
}
