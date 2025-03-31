package com.kospot.application.multiGame.gameRoom;

import com.kospot.domain.multiGame.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.global.annotation.usecase.UseCase;
import com.kospot.presentation.multiGame.gameRoom.dto.response.GameRoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindDetailGameRoomUseCase {

    private final GameRoomAdaptor gameRoomAdaptor;

    public GameRoomResponse execute(Long gameRoomId) {
        return null;
    }

}
