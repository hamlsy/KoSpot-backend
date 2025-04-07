package com.kospot.domain.multiGame.roundResult.service;

import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

public abstract class BaseRoundResultService<T, P> {

    /**
     * 라운드 결과를 처리하는 공통 메서드
     * @param gameId 게임 ID
     * @param roundNumber 라운드 번호
     * @param playerResults 플레이어 결과 목록
     * @return 처리된 라운드 결과
     */
    @Transactional
    public abstract T processRoundResults(Long gameId, Integer roundNumber, List<Map<String, Object>> playerResults);
    
    /**
     * 게임 ID로 전체 라운드 결과 조회
     * @param gameId 게임 ID
     * @return 라운드 번호별 플레이어 결과 맵
     */
    @Transactional(readOnly = true)
    public abstract Map<Integer, List<P>> getRoundResultsByGameId(Long gameId);
    
    /**
     * 현재 라운드 결과 조회
     * @param gameId 게임 ID
     * @param roundNumber 라운드 번호
     * @return 현재 라운드의 플레이어 결과 목록
     */
    @Transactional(readOnly = true)
    public abstract List<P> getCurrentRoundResults(Long gameId, Integer roundNumber);
    
    /**
     * 플레이어가 이 라운드에 제출했는지 확인
     * @param gameId 게임 ID
     * @param roundNumber 라운드 번호
     * @param gamePlayer 게임 플레이어
     * @return 제출 여부
     */
    @Transactional(readOnly = true)
    public abstract boolean hasPlayerSubmitted(Long gameId, Integer roundNumber, GamePlayer gamePlayer);
} 