package com.kospot.domain.multi.submission.event;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 플레이어/팀 제출 완료 이벤트
 * 
 * 목적:
 * - 비동기 조기 종료 체크 트리거
 * - 모든 참가자가 제출 완료했는지 확인
 * 
 * 사용처:
 * - SubmitRoadViewPlayerAnswerUseCase
 * - SubmitRoadViewTeamAnswerUseCase (향후)
 */
@Getter
@AllArgsConstructor
public class PlayerSubmissionCompletedEvent {

    private final String gameRoomId;
    private final GameMode mode;
    private final PlayerMatchType matchType;
    private final Long gameId;
    private final Long roundId;

}
