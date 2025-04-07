package com.kospot.domain.multiGame.roundResult.service;

import com.kospot.domain.multiGame.game.entity.MultiPhotoGame;
import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multiGame.gameRound.entity.PhotoGameRound;
import com.kospot.domain.multiGame.gameRound.repository.PhotoGameRoundRepository;
import com.kospot.domain.multiGame.roundResult.entity.PhotoPlayerRoundResult;
import com.kospot.domain.multiGame.roundResult.entity.PhotoRoundResult;
import com.kospot.exception.BaseException;
import com.kospot.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhotoRoundResultService extends BaseRoundResultService<PhotoRoundResult, PhotoPlayerRoundResult> {

    private final PhotoRoundResultRepository photoRoundResultRepository;
    private final PhotoPlayerRoundResultRepository playerRoundResultRepository;
    private final PhotoGameRoundRepository photoGameRoundRepository;

    @Override
    @Transactional
    public PhotoRoundResult processRoundResults(Long gameId, Integer roundNumber, List<Map<String, Object>> playerResults) {
        // 1. 게임과 라운드 정보 조회
        MultiPhotoGame game = photoRoundResultRepository.findGameById(gameId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GAME_NOT_FOUND));
                
        PhotoGameRound round = photoGameRoundRepository.findByMultiPhotoGameIdAndRoundNumber(gameId, roundNumber)
                .orElseThrow(() -> new BaseException(ErrorStatus.ROUND_NOT_FOUND));

        // 2. 라운드 결과 생성
        PhotoRoundResult roundResult = PhotoRoundResult.createRoundResult(game, round, roundNumber);

        // 3. 플레이어 결과 처리
        // 포토 모드: 프론트엔드에서 전달한 rank와 isCorrect 사용
        playerResults.forEach(result -> {
            PhotoPlayerRoundResult playerResult = PhotoPlayerRoundResult.createResult(
                    (GamePlayer) result.get("gamePlayer"),
                    (Long) result.get("submissionTime"),
                    (Boolean) result.get("isCorrect"),
                    (Integer) result.get("rank"),
                    (Integer) result.get("answerOrder"),
                    (Integer) result.get("teamNumber")
            );
            roundResult.addPlayerResult(playerResult);
            playerResult.calculateScore();
        });

        // 4. 결과 저장
        roundResult.markAsProcessed();
        return photoRoundResultRepository.save(roundResult);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, List<PhotoPlayerRoundResult>> getRoundResultsByGameId(Long gameId) {
        return photoRoundResultRepository.findByGameId(gameId)
                .stream()
                .collect(Collectors.groupingBy(
                        PhotoRoundResult::getRoundNumber,
                        Collectors.flatMapping(
                                result -> result.getPlayerResults().stream(),
                                Collectors.toList()
                        )
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhotoPlayerRoundResult> getCurrentRoundResults(Long gameId, Integer roundNumber) {
        return photoRoundResultRepository.findByGameIdAndRoundNumber(gameId, roundNumber)
                .map(PhotoRoundResult::getPlayerResults)
                .orElse(List.of());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasPlayerSubmitted(Long gameId, Integer roundNumber, GamePlayer gamePlayer) {
        return playerRoundResultRepository.existsByGameIdAndRoundNumberAndGamePlayer(gameId, roundNumber, gamePlayer);
    }
} 