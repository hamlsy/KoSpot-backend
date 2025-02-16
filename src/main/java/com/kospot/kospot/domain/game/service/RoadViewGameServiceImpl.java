package com.kospot.kospot.domain.game.service;

import com.kospot.kospot.domain.game.repository.RoadViewGameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoadViewGameServiceImpl implements RoadViewGameService{

    private RoadViewGameRepository repository;


}
