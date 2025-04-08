//package com.kospot.domain.multiGame.roundResult.service;
//
//import com.kospot.domain.multiGame.game.entity.MultiRoadViewGame;
//import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
//import com.kospot.domain.multiGame.gameRound.entity.RoadViewGameRound;
//import com.kospot.domain.multiGame.gameRound.repository.RoadViewGameRoundRepository;
//import com.kospot.domain.multiGame.roundResult.entity.RoadViewPlayerRoundResult;
//import com.kospot.domain.multiGame.roundResult.entity.RoadViewRoundResult;
//import com.kospot.domain.multiGame.roundResult.repository.RoadViewPlayerRoundResultRepository;
//import com.kospot.domain.multiGame.roundResult.repository.RoadViewRoundResultRepository;
//import com.kospot.exception.BaseException;
//import com.kospot.exception.payload.code.ErrorStatus;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Comparator;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class RoadViewRoundResultService extends BaseRoundResultService<RoadViewRoundResult, RoadViewPlayerRoundResult> {
//
//    private final RoadViewRoundResultRepository roadViewRoundResultRepository;
//    private final RoadViewPlayerRoundResultRepository playerRoundResultRepository;
//    private final RoadViewGameRoundRepository roadViewGameRoundRepository;
//
//    @Override
//    @Transactional
//    public RoadViewRoundResult processRoundResults(Long gameId, Integer roundNumber, List<Map<String, Object>> playerResults) {
//        // 1. 게임과 라운드 정보 조회
//        MultiRoadViewGame game = roadViewRoundResultRepository.findGameById(gameId)
//                .orElseThrow(() -> new BaseException(ErrorStatus.GAME_NOT_FOUND));
//
//        RoadViewGameRound round = roadViewGameRoundRepository.findByMultiRoadViewGameIdAndRoundNumber(gameId, roundNumber)
//                .orElseThrow(() -> new BaseException(ErrorStatus.ROUND_NOT_FOUND));
//
//        // 2. 라운드 결과 생성
//        RoadViewRoundResult roundResult = RoadViewRoundResult.createRoundResult(game, round, roundNumber);
//
//        // 3. 플레이어 결과 처리
//        List<RoadViewPlayerRoundResult> results = playerResults.stream()
//                .map(result -> RoadViewPlayerRoundResult.createResult(
//                        (GamePlayer) result.get("gamePlayer"),
//                        (Long) result.get("submissionTime"),
//                        (Boolean) result.get("isCorrect"),
//                        0, // 임시 순위 (후에 계산)
//                        (Double) result.get("distance"),
//                        (Integer) result.get("teamNumber")
//                ))
//                .collect(Collectors.toList());
//
//        // 정답인 결과만 필터링하고 거리순으로 정렬
//        List<RoadViewPlayerRoundResult> correctResults = results.stream()
//                .filter(RoadViewPlayerRoundResult::getIsCorrect)
//                .sorted(Comparator.comparing(RoadViewPlayerRoundResult::getDistance))
//                .collect(Collectors.toList());
//
//        // 순위 부여
//        for (int i = 0; i < correctResults.size(); i++) {
//            correctResults.get(i).assignRank(i + 1);
//        }
//
//        // 결과 추가 및 점수 계산
//        results.forEach(result -> {
//            roundResult.addPlayerResult(result);
//            result.calculateScore();
//        });
//
//        // 4. 결과 저장
//        roundResult.markAsProcessed();
//        return roadViewRoundResultRepository.save(roundResult);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Map<Integer, List<RoadViewPlayerRoundResult>> getRoundResultsByGameId(Long gameId) {
//        return roadViewRoundResultRepository.findByGameId(gameId)
//                .stream()
//                .collect(Collectors.groupingBy(
//                        RoadViewRoundResult::getRoundNumber,
//                        Collectors.flatMapping(
//                                result -> result.getPlayerResults().stream(),
//                                Collectors.toList()
//                        )
//                ));
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<RoadViewPlayerRoundResult> getCurrentRoundResults(Long gameId, Integer roundNumber) {
//        return roadViewRoundResultRepository.findByGameIdAndRoundNumber(gameId, roundNumber)
//                .map(RoadViewRoundResult::getPlayerResults)
//                .orElse(List.of());
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public boolean hasPlayerSubmitted(Long gameId, Integer roundNumber, GamePlayer gamePlayer) {
//        return playerRoundResultRepository.existsByGameIdAndRoundNumberAndGamePlayer(gameId, roundNumber, gamePlayer);
//    }
//}