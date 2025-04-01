package com.kospot.domain.multiGame.game.entity;


import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.coordinate.entity.Coordinate;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public class Round extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private MultiRoadViewGame multiRoadViewGame;

    private Integer roundNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinate_id")
    private Coordinate targetCoordinate;

    //todo player

}
