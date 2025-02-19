package com.kospot.kospot.domain.point.adaptor;

import com.kospot.kospot.domain.game.entity.GameMode;
import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.point.entity.PointHistory;

import java.util.List;

public interface PointHistoryAdaptor {
    List<PointHistory> getAllHistoryByMember(Member member);

    List<PointHistory> getGameHistoryByMember(Member member, GameType gameType);

    List<PointHistory> getHistoryByMemberAndGameMode(Member member, GameType gameType, GameMode gameMode);
}
