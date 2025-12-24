# KoSpot-backend

## 프로젝트 소개

KoSpot은 대한민국 내 랜드마크 및 관광지를 기반으로 한 위치 추론 게임 서비스입니다. 유저는 다양한 게임 모드를 통해 랜드마크를 맞히고, 랭크 시스템을 통해 경쟁할 수 있습니다.

이 백엔드는 Spring Boot를 기반으로 구현되었으며, 게임 데이터 관리, 랭킹 시스템, 사용자 기록 저장 및 조회 등의 기능을 제공합니다.

## 기술 스택

- 🚀 **Backend Framework**:  Spring Boot
- 🗄️ **Database**:  MySQL 8, Spring Data JPA
- ☁️ **Cloud & Deployment**: 
- ⚡ **Caching**: Redis
- 🔐 **Authentication**: 
- 🛠 **Build & CI/CD**: 

## 주요 기능

### 메인

### 상점

### 게임
- 게임 기록 저장 및 통계 제공
- 랭킹 시스템 (포인트 기반 랭크 계산)

### 관리
- 관광지 좌표 데이터 저장 및 조회


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

### 4. 비정상 종료시 게임 방 비즈니스 로직 정리

- **이슈**: [#128 방장 재지정 기능 구현](https://github.com/hamlsy/KoSpot-backend/issues/128)[PR](https://github.com/hamlsy/KoSpot-backend/pull/132)
- **개선 내용**:
  1. 기존 Read and Check 방식에서 분산락 방식으로 변경함
  2. 방장 재 지정시 이미 퇴장한 플레이어에게 방장이 부여되는 현상을 개선함

## 프로젝트 구조

```bash
kospot-backend/
├── src/main/java/com/kospot/kospot/
│   ├── application/        # UseCase 계층
│   ├── domain/    # entity, dto, service 계층
│   ├── exception/       # exception 계층
│   ├── global/    # aop, config 클래스
│   ├── presentation/        # Controller 계층
│
├── src/main/resources/
│   ├── application.yml # 환경설정 파일
│   └── data/excel/ # 좌표 데이터 파일
└── Dockerfile         # Docker 빌드 파일 
```


---

[이메일](dltmddud1122@naver.com)

