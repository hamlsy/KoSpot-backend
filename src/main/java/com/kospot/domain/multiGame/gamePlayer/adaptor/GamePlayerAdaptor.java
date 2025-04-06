package com.kospot.domain.multiGame.gamePlayer.adaptor;

import com.kospot.domain.multiGame.gamePlayer.repository.GamePlayerRepository;
import com.kospot.global.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Adaptor
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GamePlayerAdaptor {

    private final GamePlayerRepository repository;

}
