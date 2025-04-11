package com.kospot.domain.multiGame.game.service;

import com.kospot.domain.multiGame.game.entity.MultiPhotoGame;
import com.kospot.domain.multiGame.gameRound.entity.PhotoGameRound;
import com.kospot.domain.multiGame.gameRound.repository.PhotoGameRoundRepository;
import com.kospot.domain.multiGame.submission.entity.photo.PhotoPlayerSubmission;
import com.kospot.domain.multiGame.game.repository.MultiPhotoGameRepository;
import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multiGame.gameRoom.entity.GameRoom;
import com.kospot.exception.object.domain.GameHandler;
import com.kospot.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PhotoGameService {

    private final MultiPhotoGameRepository multiPhotoGameRepository;
    private final PhotoGameRoundRepository photoGameRoundRepository;
    
    /**
     * 정답을 제출하는 메서드
     * 프론트엔드에서 정답 여부를 확인하고, 맞은 경우에만 호출됨
     */
    @Transactional
    public Integer submitCorrectAnswer(Long gameId, Integer roundNumber, GamePlayer gamePlayer, Integer teamNumber) {
        // 1. 게임과 현재 라운드 조회
        MultiPhotoGame game = multiPhotoGameRepository.findById(gameId)
                .orElseThrow(() -> new GameHandler(ErrorStatus.GAME_NOT_FOUND));
        
        PhotoGameRound round = photoGameRoundRepository.findByGameIdAndRoundNumber(gameId, roundNumber)
                .orElseThrow(() -> new GameHandler(ErrorStatus.GAME_NOT_FOUND));
        
        // 2. 이미 끝난 라운드인지 확인
        if (round.getIsFinished()) {
            throw new GameHandler(ErrorStatus.GAME_IS_ALREADY_COMPLETED);
        }
        
        // 3. 해당 플레이어가 이미 정답을 제출했는지 확인
        boolean alreadySubmitted = round.getPlayerSubmissions().stream()
                .anyMatch(submission -> submission.getGamePlayer().getId().equals(gamePlayer.getId()));
        
        if (alreadySubmitted) {
            throw new GameHandler(ErrorStatus._BAD_REQUEST);
        }
        
        // 4. 정답 제출 정보 생성
        PhotoPlayerSubmission submission = PhotoPlayerSubmission.createSubmission(
                gamePlayer,
                Instant.now().toEpochMilli(),
                teamNumber
        );
        
        // 5. 정답 순서 할당 및 저장
        Integer answerOrder = round.addCorrectAnswer(submission);
        
        // 6. 모든 플레이어가 정답을 제출했는지 확인하고 라운드 종료 여부 결정
        GameRoom gameRoom = game.getGameRoom();
        boolean allAnswered = round.allPlayersAnsweredCorrectly(gameRoom.getCurrentPlayerCount());
        
        if (allAnswered) {
            round.finishRound();
            
            // 마지막 라운드인지 확인하고 게임 종료 처리
            if (game.isLastRound()) {
                game.finishGame();
            } else {
                // 다음 라운드로 이동
                game.moveToNextRound();
            }
        }
        
        return answerOrder;
    }
} 