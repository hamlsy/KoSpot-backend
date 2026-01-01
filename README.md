# KoSpot-backend

## 프로젝트 소개

KoSpot은 대한민국 내 랜드마크와 관광지를 기반으로 한 위치 추론 게임 서비스입니다. 
사용자들이 스트리트뷰나 사진을 보고 정확한 위치를 맞추는 게임을 플레이할 수 있으며, 
싱글 플레이와 멀티플레이 모드를 지원합니다.

## 기술 스택

- **Backend Framework**: Spring Boot 3.4.1
- **Language**: Java 17
- **Database**: MySQL 8, Spring Data JPA
- **Caching**: Redis, Redisson (분산 락)
- **WebSocket**: Spring WebSocket (STOMP)
- **Authentication**: Spring Security OAuth2 (Google, Naver, Kakao), JWT
- **File Storage**: AWS S3
- **Build Tool**: Gradle
- **API Documentation**: Springdoc OpenAPI (Swagger)

## 주요 기능

### 게임
- **싱글 플레이**
  - 로드뷰 연습 모드: 지역별 연습 플레이
  - 로드뷰 랭크 모드: 랭크 점수 변동
  - 게임 기록 저장 및 통계 제공
- **멀티 플레이**
  - 실시간 게임 방 생성 및 참가
  - 개인전/팀전 모드 지원
  - WebSocket 기반 실시간 통신
  - 라운드별 정답 제출 및 점수 계산
- **랭킹 시스템**
  - 8단계 티어 시스템 (BRONZE ~ CHALLENGER)
  - 게임 점수 기반 레이팅 계산
  - 티어별 포인트 배수 적용

### 상점
- 마커, 프로필, 이펙트, 테마 아이템 판매
- 포인트 기반 아이템 구매
- 인벤토리 관리 및 아이템 장착

### 포인트 시스템
- 게임 결과에 따른 포인트 획득
- 티어별 포인트 배수 차등 적용
- 포인트 사용 내역 기록

### 관리 기능
- 관광지 좌표 데이터 관리 (엑셀 일괄 등록 지원)
- 게임 모드 활성화/비활성화 설정
- 공지사항 및 배너 관리
- 회원 관리 및 통계 조회

## 성능 개선 및 버그 해결

### 1. 게임 종료 로직 최적화

- **이슈**: [#35 게임 종료 로직 최적화](https://github.com/hamlsy/KoSpot-backend/issues/35)
- **개선 내용**:
  1. Async를 통해 서브 로직을 비동기로 실행하므로 주요 로직의 응답 속도를 개선함. (TEST_SIZE = 150, 1400ms -> 3ms)
  2. 서브 로직과 주요 로직의 트랜잭션을 분리하여 서브 로직의 예외가 주요 로직에 영향을 끼치지 않음.

### 2. 비정상 종료시 게임 방 비즈니스 로직 정리

- **이슈**: [#71 소켓 비정상 연결 끊김시 비즈니스 로직 처리](https://github.com/hamlsy/KoSpot-backend/issues/71)
- **개선 내용**:
  1. Interceptor 및 SessionDisconnectEvent Handler를 통해 네트워크 끊김에도 게임 방에 플레이어가 남아 있던 현상을 수정함
  2. 추후 Interceptor 대신 Session Event Handler를 통해 비즈니스 로직 처리로 변경함.

### 3. 정답 제출 Redis 동시성 문제 해결

- **이슈**: [#126 정답 제출 Redis 동시성 문제 해결](https://github.com/hamlsy/KoSpot-backend/issues/126)
- **개선 내용**:
  1. DB 검증과 Redis 불일치를 DB검증 대신 Redis 원자적 연산(count) 로직으로 변경함
  2. 모든 플레이어가 정답을 동시에 제출했을 때 게임이 끝나지 않는 현상 발생을 해결함

### 4. 방장 재지정시 Race condition 문제 개선

- **이슈**: [#128 방장 재지정 기능 구현](https://github.com/hamlsy/KoSpot-backend/issues/128)[PR](https://github.com/hamlsy/KoSpot-backend/pull/132)
- **개선 내용**:
  1. 기존 Read and Check 방식에서 분산락 방식으로 변경함
  2. 방장 재 지정시 이미 퇴장한 플레이어에게 방장이 부여되는 현상을 개선함

## 프로젝트 구조

```
kospot-backend/
├── src/main/java/com/kospot/
│   ├── application/           # UseCase 계층 (비즈니스 로직 오케스트레이션)
│   ├── domain/                # 도메인 계층 (Entity, Service, Repository)
│   ├── infrastructure/        # 인프라 계층 (Config, Security, Exception)
│   ├── presentation/          # Controller 계층 (REST API, WebSocket)
│
├── src/main/resources/
│   ├── application.yml        # 환경설정 파일
│   └── data/excel/            # 좌표 데이터 파일
│
├── docs/                      # API 문서 및 설계 문서
└── Dockerfile                 # Docker 빌드 파일
```

## 아키텍처

- **Domain-Driven Design (DDD)** 기반 계층 구조
- UseCase 레이어를 통한 비즈니스 로직 분리
- Redis를 활용한 실시간 데이터 관리 및 캐싱
- WebSocket(STOMP) 기반 실시간 멀티플레이 지원

---

문의: dltmddud1122@naver.com

