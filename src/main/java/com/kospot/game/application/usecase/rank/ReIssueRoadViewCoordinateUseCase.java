package com.kospot.game.application.usecase.rank;

import com.kospot.coordinate.application.adaptor.CoordinateAdaptor;
import com.kospot.coordinate.domain.entity.Coordinate;
import com.kospot.coordinate.domain.entity.Sido;
import com.kospot.coordinate.application.service.CoordinateService;
import com.kospot.game.application.adaptor.RoadViewGameAdaptor;
import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.game.application.service.AESService;
import com.kospot.game.application.service.RoadViewGameService;
import com.kospot.game.domain.vo.GameType;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.game.presentation.dto.response.StartGameResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class ReIssueRoadViewCoordinateUseCase {

    private final MemberAdaptor memberAdaptor;
    private final RoadViewGameAdaptor roadViewGameAdaptor;
    private final RoadViewGameService roadViewGameService;
    private final CoordinateService coordinateService;
    private final CoordinateAdaptor coordinateAdaptor;
    private final AESService aesService;

    public StartGameResponse.ReIssue execute(Long memberId, Long gameId){
        Member member = memberAdaptor.queryById(memberId);
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
