package com.kospot.kospot.application.multiGame.gameRoom;

import com.kospot.kospot.domain.multiGame.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import com.kospot.kospot.presentation.multiGame.gameRoom.dto.request.GameRoomRequest;
import com.kospot.kospot.presentation.multiGame.gameRoom.dto.response.FindGameRoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindAllGameRoomUseCase {

    private final GameRoomAdaptor gameRoomAdaptor;
    private static final int SIZE = 15;

    public List<FindGameRoomResponse> execute(GameRoomRequest.Find request, int page) {
        Pageable pageable = PageRequest.of(page, SIZE, Sort.Direction.DESC, "createdDate");
        return gameRoomAdaptor.queryAllByKeyword(request.getKeyword(), pageable);
    }

}
