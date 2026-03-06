package com.kospot.multi.room.application.usecase;

import com.kospot.multi.room.adaptor.GameRoomAdaptor;
import com.kospot.multi.room.domain.entity.GameRoom;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.common.redis.domain.multi.room.adaptor.GameRoomRedisAdaptor;
import com.kospot.multi.room.presentation.dto.response.FindGameRoomResponse;
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
    private final GameRoomRedisAdaptor gameRoomRedisAdaptor;

    private static final String SORT_PROPERTIES = "createdDate";
    private static final int SIZE = 10;

    public List<FindGameRoomResponse> execute(int page) {
        Pageable pageable = PageRequest.of(page, SIZE, Sort.Direction.DESC, SORT_PROPERTIES);
        List<GameRoom> gameRooms = gameRoomAdaptor.queryAllWithWaitingFirst(pageable);

        return gameRooms.stream().map(
                r -> FindGameRoomResponse.from(r, gameRoomRedisAdaptor.getCurrentPlayersCount(r.getId().toString())))
                .collect(Collectors.toList());
    }

}
