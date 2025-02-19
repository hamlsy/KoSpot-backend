package com.kospot.kospot.domain.point.adaptor;


import com.kospot.kospot.domain.game.entity.GameMode;
import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.point.entity.PointHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PointHistoryAdaptorImpl implements PointHistoryAdaptor{
    @Override
    public List<PointHistory> getAllHistoryByMember(Member member) {
        return List.of();
    }

    @Override
    public List<PointHistory> getGameHistoryByMember(Member member, GameType gameType) {
        return List.of();
    }

    @Override
    public List<PointHistory> getHistoryByMemberAndGameMode(Member member, GameType gameType, GameMode gameMode) {
        return List.of();
    }
}
