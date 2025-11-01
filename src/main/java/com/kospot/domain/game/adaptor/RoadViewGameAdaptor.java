package com.kospot.domain.game.adaptor;

import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.game.repository.RoadViewGameRepository;
import com.kospot.domain.game.vo.GameStatus;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.exception.object.domain.GameHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Adaptor
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RoadViewGameAdaptor {

    private final RoadViewGameRepository repository;

    public RoadViewGame queryById(Long gameId) {
        return repository.findById(gameId).orElseThrow(
                () -> new GameHandler(ErrorStatus.GAME_NOT_FOUND)
        );
    }

    public RoadViewGame queryByIdFetchCoordinate(Long gameId) {
        return repository.findByIdFetchCoordinate(gameId).orElseThrow(
                () -> new GameHandler(ErrorStatus.GAME_NOT_FOUND)
        );
    }

    public List<RoadViewGame> queryRecentThreeGamesByMember(Member member) {
        Pageable pageable = PageRequest.of(0, 3);
        return repository.findTop3ByMemberAndGameStatusOrderByCreatedAtDesc(
                member,
                GameStatus.COMPLETED,
                pageable
        );
    }

    public Page<RoadViewGame> queryAllGamesByMember(Member member, Pageable pageable) {
        return repository.findByMemberAndGameStatusOrderByCreatedAtDesc(
                member,
                GameStatus.COMPLETED,
                pageable
        );
    }
}
