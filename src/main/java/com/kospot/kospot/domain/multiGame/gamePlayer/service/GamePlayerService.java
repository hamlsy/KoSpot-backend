package com.kospot.kospot.domain.multiGame.gamePlayer.service;

import com.kospot.kospot.domain.multiGame.gamePlayer.repository.GamePlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GamePlayerService {

    private final GamePlayerRepository gamePlayerRepository;

}
