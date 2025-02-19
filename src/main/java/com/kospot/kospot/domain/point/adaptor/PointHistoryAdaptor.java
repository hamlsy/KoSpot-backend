package com.kospot.kospot.domain.point.adaptor;

import com.kospot.kospot.domain.game.entity.GameMode;
import com.kospot.kospot.domain.game.entity.GameType;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.point.entity.PointHistory;

import java.util.List;

public interface PointHistoryAdaptor {
    List<PointHistory> queryAllHistoryByMemberId(Long memberId);

    List<PointHistory> queryGameHistoryByMemberId(Long memberId, GameType gameType);

    List<PointHistory> queryHistoryByMemberIdAndGameMode(Long memberId, GameType gameType, GameMode gameMode);
}
