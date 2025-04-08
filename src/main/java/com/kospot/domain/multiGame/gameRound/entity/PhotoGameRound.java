package com.kospot.domain.multiGame.gameRound.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.multiGame.game.entity.MultiPhotoGame;
import com.kospot.domain.multiGame.submittion.entity.PhotoPlayerSubmission;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhotoGameRound extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Integer roundNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "multi_photo_game_id")
    private MultiPhotoGame multiPhotoGame;
    
    // 라운드에 사용되는 사진들의 URL 목록
    @ElementCollection
    @CollectionTable(name = "photo_game_round_images", 
                    joinColumns = @JoinColumn(name = "photo_game_round_id"))
    @Column(name = "image_url")
    private List<String> photoUrls = new ArrayList<>();
    
    // 정답 지역 (도시 이름)
    private String correctLocation;
    
    // 현재 정답자 수 
    private Integer correctAnswersCount = 0;
    
    @OneToMany(mappedBy = "photoGameRound", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PhotoPlayerSubmission> playerSubmissions = new ArrayList<>();
    
    private Boolean isFinished;
    
    // Business methods
    public void setMultiPhotoGame(MultiPhotoGame multiPhotoGame) {
        this.multiPhotoGame = multiPhotoGame;
    }
    
    /** todo
     * 새로운 정답자를 추가하고 정답 순서를 반환
     * 정답 처리는 프론트에서 이미 했다고 가정
     * 동시성 문제를 방지하기 위해 AtomicInteger를 사용 (실제 구현에서는 락이나 트랜잭션 처리 필요)
     */
    public synchronized Integer addCorrectAnswer(PhotoPlayerSubmission submission) {
        // 정답자 순서 할당 (1부터 시작)
        correctAnswersCount++;
        
        // 정답 순서 부여
        submission.assignAnswerOrder(correctAnswersCount);
        
        // 제출 정보 저장
        this.playerSubmissions.add(submission);
        submission.setPhotoGameRound(this);
        
        return correctAnswersCount;
    }
    
    public void finishRound() {
        this.isFinished = true;
    }
    
    // 모든 플레이어가 정답을 맞췄는지 확인
    public boolean allPlayersAnsweredCorrectly(int totalPlayers) {
        return correctAnswersCount >= totalPlayers;
    }
    
    // 생성 메서드
    public static PhotoGameRound createRound(Integer roundNumber, String correctLocation, List<String> photoUrls) {
        return PhotoGameRound.builder()
                .roundNumber(roundNumber)
                .correctLocation(correctLocation)
                .photoUrls(photoUrls)
                .correctAnswersCount(0)
                .isFinished(false)
                .build();
    }
} 