package com.kospot.kospot.domain.multiplay.gameRoom.adaptor;

import com.kospot.kospot.domain.multiplay.gameRoom.entity.GameRoom;
import com.kospot.kospot.domain.multiplay.gameRoom.repository.GameRoomRepository;
import com.kospot.kospot.global.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Adaptor
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameRoomAdaptor {

    private final GameRoomRepository gameRoomRepository;


}
