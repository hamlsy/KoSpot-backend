package com.kospot.application.multiplayer.gameroom;

import com.kospot.domain.multigame.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.multigame.gameroom.dto.response.FindGameRoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindAllGameRoomUseCase {

    private final GameRoomAdaptor gameRoomAdaptor;

    private static final String SORT_PROPERTIES = "createdDate";
    private static final int SIZE = 10;

    public List<FindGameRoomResponse> execute(int page) {
        Pageable pageable = PageRequest.of(page, SIZE, Sort.Direction.DESC, SORT_PROPERTIES);
        List<GameRoom> gameRooms = gameRoomAdaptor.queryAllWithWaitingFirst(pageable);

        return gameRooms.stream().map(FindGameRoomResponse::from)
                .collect(Collectors.toList());
    }

}
