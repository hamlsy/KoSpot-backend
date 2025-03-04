# KoSpot-backend

## 프로젝트 소개

KoSpot은 대한민국 내 랜드마크 및 관광지를 기반으로 한 위치 추론 게임 서비스입니다. 유저는 다양한 게임 모드를 통해 랜드마크를 맞히고, 랭크 시스템을 통해 경쟁할 수 있습니다.

이 백엔드는 Spring Boot를 기반으로 구현되었으며, 게임 데이터 관리, 랭킹 시스템, 사용자 기록 저장 및 조회 등의 기능을 제공합니다.

## 기술 스택

- 🚀 **Backend Framework**:  Spring Boot
- 🗄️ **Database**:  MySQL 8, Spring Data JPA
- ☁️ **Cloud & Deployment**: 
- ⚡ **Caching**:  
- ❌ **Message Queue**: 
- 🔐 **Authentication**:  
- 🛠 **Build & CI/CD**: 

## 주요 기능

### 

### 게임
- 게임 기록 저장 및 통계 제공
- 랭킹 시스템 (포인트 기반 랭크 계산)
- 
### 관리
- 관광지 좌표 데이터 저장 및 조회


## 성능 개선

KoSpot에서는 대용량 데이터 처리와 랜덤 조회 성능을 최적화하기 위해 여러 가지 성능 개선 작업을 수행했습니다.

### 1. 랜덤 좌표 조회 성능 개선

- **ISSUE**: [#12 랜덤 좌표 조회 성능 최적화](https://github.com/hamlsy/KoSpot-backend/issues/17)
- **개선 내용**: MySQL에서 ORDER BY RAND() 대신 ID 범위 샘플링 기법 적용, 캐시테이블로 ID 범위 계산 최적화, 지역별 테이블 분리로 캐시 미스 최소화

### 2. Bulk Insert 최적화

- **이슈**: 
- **PR**: 
- **개선 내용**: 

### 3. 게임 종료 로직 최적화

- **이슈**: [#35 게임 종료 로직 최적화](https://github.com/hamlsy/KoSpot-backend/issues/35)
- **PR**: 
- **개선 내용**: 

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
└── Dockerfile         # Docker 빌드 파일 (예정)
```

## API 문서

API 명세는 Swagger를 통해 제공됩니다.

- **Swagger UI**: `http://localhost:8080/swagger-ui.html` (변경 예정)

## 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다.

---

문의: [이메일](dltmddud1122@naver.com)

