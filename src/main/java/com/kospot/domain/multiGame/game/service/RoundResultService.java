//package com.kospot.domain.multiGame.game.service;
//
//import com.kospot.domain.multiGame.game.entity.MultiGame;
//import com.kospot.domain.multiGame.roundResult.entity.PlayerRoundResult;
//import com.kospot.domain.multiGame.roundResult.entity.RoundResult;
//import com.kospot.domain.multiGame.roundResult.repository.RoundResultRepository;
//import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
//import com.kospot.exception.object.domain.GameHandler;
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
//public class RoundResultService {
//
//    private final RoundResultRepository roundResultRepository;
//
//    @Transactional
//    public RoundResult processRoundResults(Long gameId, Integer roundNumber, List<Map<String, Object>> playerResults) {
//        // 1. 라운드 결과 생성
//        MultiGame game = roundResultRepository.findGameById(gameId)
//                .orElseThrow(() -> new GameHandler(ErrorStatus.GAME_NOT_FOUND));
//
//        RoundResult roundResult = RoundResult.createRoundResult(game, roundNumber);
//
//        // 2. 플레이어 결과 처리
//        // 로드뷰 모드인 경우, 거리에 따라 정렬하여 순위 부여
//        if (game.isPhotoMode()) {
//            // 포토 모드: 프론트엔드에서 전달한 rank 사용
//            playerResults.forEach(result -> {
//                PlayerRoundResult playerResult = PlayerRoundResult.createResult(
//                        (GamePlayer) result.get("gamePlayer"),
//                        (Long) result.get("submissionTime"),
//                        (Boolean) result.get("isCorrect"),
//                        (Integer) result.get("rank"),
//                        (Double) result.get("distance"),
//                        (Integer) result.get("teamNumber")
//                );
//                roundResult.addPlayerResult(playerResult);
//            });
//        } else {
//            // 로드뷰 모드: 거리에 따라 정렬하여 순위 부여
//            List<PlayerRoundResult> results = playerResults.stream()
//                    .map(result -> PlayerRoundResult.createResult(
//                            (GamePlayer) result.get("gamePlayer"),
//                            (Long) result.get("submissionTime"),
//                            (Boolean) result.get("isCorrect"),
//                            0, // 임시 순위 (후에 계산)
//                            (Double) result.get("distance"),
//                            (Integer) result.get("teamNumber")
//                    ))
//                    .collect(Collectors.toList());
//
//            // 정답인 결과만 필터링하고 거리순으로 정렬
//            List<PlayerRoundResult> correctResults = results.stream()
//                    .filter(PlayerRoundResult::getIsCorrect)
//                    .sorted(Comparator.comparing(PlayerRoundResult::getDistance))
//                    .collect(Collectors.toList());
//
//            // 순위 부여
//            for (int i = 0; i < correctResults.size(); i++) {
//                correctResults.get(i).assignRank(i + 1);
//            }
//
//            // 결과 추가
//            results.forEach(roundResult::addPlayerResult);
//        }
//
//        // 3. 점수 계산
//        roundResult.getPlayerResults().forEach(PlayerRoundResult::calculateScore);
//
//        // 4. 결과 저장
//        return roundResultRepository.save(roundResult);
//    }
//
//    @Transactional(readOnly = true)
//    public Map<Integer, List<PlayerRoundResult>> getRoundResultsByGameId(Long gameId) {
//        return roundResultRepository.findByGameId(gameId)
//                .stream()
//                .collect(Collectors.groupingBy(
//                        RoundResult::getRoundNumber,
//                        Collectors.flatMapping(
//                                result -> result.getPlayerResults().stream(),
//                                Collectors.toList()
//                        )
//                ));
//    }
//
//    @Transactional(readOnly = true)
//    public List<PlayerRoundResult> getCurrentRoundResults(Long gameId, Integer roundNumber) {
//        return roundResultRepository.findByGameIdAndRoundNumber(gameId, roundNumber)
//                .map(RoundResult::getPlayerResults)
//                .orElse(List.of());
//    }
//}