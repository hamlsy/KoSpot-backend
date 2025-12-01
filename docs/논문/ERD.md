# KoSpot ERD (Entity Relationship Diagram)

## ERD 다이어그램

```mermaid
erDiagram
    Member ||--o{ GameRoom : "creates/hosts"
    Member ||--o{ GamePlayer : "plays"
    Member ||--|| MemberStatistic : "has"
    Member ||--o{ MemberItem : "owns"
    Member ||--o{ GameRank : "has"
    Member }o--|| Image : "equippedMarker"
    
    GameRoom ||--|| MultiRoadViewGame : "has"
    GameRoom }o--|| Member : "host"
    
    MultiRoadViewGame ||--o{ RoadViewGameRound : "contains"
    MultiRoadViewGame ||--o{ GamePlayer : "has"
    
    RoadViewGameRound }o--|| Coordinate : "target"
    
    GamePlayer }o--|| Member : "member"
    GamePlayer }o--|| MultiRoadViewGame : "game"
    
    Coordinate ||--o{ RoadViewGameRound : "used_in"
    
    Item ||--o{ MemberItem : "purchased_as"
    Item }o--|| Image : "image"
    
    MemberStatistic ||--o{ GameModeStatistic : "contains"
    
    Member {
        bigint member_id PK
        string username UK
        string nickname UK
        string email
        int point
        bigint image_id FK
        bigint game_room_id FK
        enum role
        boolean first_visited
        datetime created_at
        datetime updated_at
    }
    
    GameRoom {
        bigint id PK
        string title
        enum game_mode
        enum player_match_type
        int time_limit
        boolean private_room
        int max_players
        int team_count
        string password
        boolean is_poi_name_visible
        enum status
        bigint host_id FK
        datetime created_at
        datetime updated_at
    }
    
    MultiRoadViewGame {
        bigint id PK
        bigint game_room_id FK
        enum match_type
        enum game_mode
        int total_rounds
        int current_round
        int time_limit
        boolean is_finished
        enum status
        datetime created_at
        datetime updated_at
    }
    
    RoadViewGameRound {
        bigint id PK
        bigint multi_road_view_game_id FK
        bigint coordinate_id FK
        int round_number
        boolean is_finished
        int time_limit
        datetime server_start_time
        datetime created_at
        datetime updated_at
    }
    
    GamePlayer {
        bigint id PK
        bigint member_id FK
        bigint multi_road_view_game_id FK
        string nickname
        int team_number
        int round_rank
        double total_score
        enum status
        string equipped_marker_image_url
    }
    
    Coordinate {
        bigint id PK
        double lat
        double lng
        string poi_name
        string sido
        string sigungu
        string eupmyeondong
        enum location_type
        boolean is_valid
        datetime created_at
        datetime updated_at
    }
    
    Item {
        bigint item_id PK
        string name
        text description
        int price
        int stock
        enum item_type
        boolean is_available
        boolean is_default
        bigint image_id FK
        datetime created_at
        datetime updated_at
    }
    
    MemberItem {
        bigint member_item_id PK
        bigint member_id FK
        bigint item_id FK
        boolean is_equipped
        datetime purchased_at
        datetime created_at
        datetime updated_at
    }
    
    GameRank {
        bigint game_rank_id PK
        bigint member_id FK
        enum game_mode
        int rating_score
        enum rank_tier
        enum rank_level
        datetime created_at
        datetime updated_at
    }
    
    MemberStatistic {
        bigint member_statistic_id PK
        bigint member_id FK UK
        int current_streak
        int max_streak
        date last_play_date
        datetime last_played_at
        datetime created_at
        datetime updated_at
    }
    
    GameModeStatistic {
        bigint game_mode_statistic_id PK
        bigint member_statistic_id FK
        enum game_mode
        int total_games
        int practice_games
        int rank_games
        int multi_games
        double best_score
        double average_score
        datetime created_at
        datetime updated_at
    }
    
    Image {
        bigint id PK
        string image_path
        string image_name
        string s3_key
        string image_url
        enum image_type
        bigint notice_id FK
        datetime created_at
        datetime updated_at
    }
```

## 주요 관계 설명

### 1. Member (회원)
- **GameRoom과의 관계**: 한 회원은 여러 게임 방을 생성할 수 있으며, 한 번에 하나의 게임 방에만 참여 가능
- **GamePlayer와의 관계**: 한 회원은 여러 게임에서 플레이어로 참여
- **MemberStatistic과의 관계**: 일대일 관계로 각 회원의 통계 정보 관리
- **GameRank와의 관계**: 게임 모드별로 랭크 정보 보유
- **MemberItem과의 관계**: 구매한 아이템 목록 관리

### 2. GameRoom (게임 방)
- **Member와의 관계**: 호스트 회원과의 일대일 관계
- **MultiRoadViewGame과의 관계**: 한 게임 방은 하나의 게임만 진행

### 3. MultiRoadViewGame (멀티 로드뷰 게임)
- **RoadViewGameRound와의 관계**: 한 게임은 여러 라운드를 포함
- **GamePlayer와의 관계**: 한 게임은 여러 플레이어를 포함

### 4. RoadViewGameRound (로드뷰 게임 라운드)
- **Coordinate와의 관계**: 각 라운드는 하나의 정답 좌표를 가짐

### 5. Coordinate (좌표)
- 여러 라운드에서 재사용 가능한 좌표 정보

### 6. Item (아이템)
- **MemberItem과의 관계**: 여러 회원이 같은 아이템을 구매할 수 있음

## 인덱스 전략

### Member 테이블
- `idx_member_username`: username 컬럼 인덱스 (로그인 조회 최적화)
- `idx_member_nickname`: nickname 컬럼 인덱스 (검색 최적화)

### Coordinate 테이블
- `idx_coordinate_sido`: sido 컬럼 인덱스 (지역별 조회 최적화)

### MemberStatistic 테이블
- `idx_member_statistic_member_id`: member_id 컬럼 인덱스 (통계 조회 최적화)

## 비정규화 전략

### RoadViewGameRound
- `server_start_time`: 타이머 동기화를 위한 서버 시작 시간을 라운드 엔티티에 직접 저장 (비정규화)
- `player_ids`: 플레이어 ID 목록을 JSON 형태로 저장 (실제 관계는 Redis에서 관리)

이러한 비정규화는 성능 최적화를 위한 것으로, 자주 조회되는 데이터를 중복 저장하여 조인 연산을 줄인다.

