package com.kospot.application.game.roadview;

import com.kospot.domain.coordinate.adaptor.CoordinateAdaptor;
import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.coordinate.entity.Sido;
import com.kospot.domain.coordinate.service.CoordinateService;
import com.kospot.domain.game.adaptor.RoadViewGameAdaptor;
import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.game.service.AESService;
import com.kospot.domain.game.service.RoadViewGameService;
import com.kospot.domain.game.vo.GameType;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.game.dto.response.StartGameResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class ReIssueRoadViewCoordinateUseCase {

    private final RoadViewGameAdaptor roadViewGameAdaptor;
    private final RoadViewGameService roadViewGameService;
    private final CoordinateService coordinateService;
    private final CoordinateAdaptor coordinateAdaptor;
    private final AESService aesService;

    public StartGameResponse.ReIssue execute(Member member, Long gameId){
        RoadViewGame game = roadViewGameAdaptor.queryByIdFetchCoordinate(gameId);
        Coordinate coordinate = game.getCoordinate();
        coordinateService.invalidateCoordinate(coordinate);
        Coordinate newCoordinate = getNewCoordinate(game.getPracticeSido());
        roadViewGameService.updateCoordinate(game, newCoordinate);

        return getEncryptedRoadViewGameResponse(game);
    }

    //encrypt -> response
    private StartGameResponse.ReIssue getEncryptedRoadViewGameResponse(RoadViewGame game) {
        return StartGameResponse.ReIssue.builder()
                .poiName(game.getGameType().equals(GameType.PRACTICE) ? game.getCoordinate().getPoiName() : "")
                .targetLat(aesService.toEncryptString(game.getCoordinate().getLat()))
                .targetLng(aesService.toEncryptString(game.getCoordinate().getLng()))
                .build();
    }

    private Coordinate getNewCoordinate(Sido sido) {
        if(sido == null) {
            return coordinateAdaptor.getRandomCoordinate();
        }
        return coordinateAdaptor.getRandomCoordinateBySido(sido);
    }


}
