package com.kospot.application.multiGame.gameRoom;

import com.kospot.domain.multiGame.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.multiGame.gameRoom.dto.response.GameRoomDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindGameRoomDetailUseCase {

    private final GameRoomAdaptor gameRoomAdaptor;

    public GameRoomDetailResponse execute(Long gameRoomId) {
        return GameRoomDetailResponse.from(
                gameRoomAdaptor.queryById(gameRoomId)
        );
    }

}
