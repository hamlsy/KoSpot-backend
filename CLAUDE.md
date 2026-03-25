# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build
./gradlew build -x test   # skip tests

# Run tests
./gradlew test

# Run single test class
./gradlew test --tests "com.kospot.game.SomeTest"

# Local development
docker-compose up -d redis   # start Redis first
./gradlew bootRun            # then run Spring Boot (uses application-local.yml)
```

## Architecture

### Domain-Driven Design with UseCase Pattern

The codebase uses a layered DDD structure. Each domain module is organized as:

```
com.kospot.<domain>/
  presentation/controller/         # REST controllers
  presentation/request/            # request DTOs
  presentation/response/           # response DTOs
  application/usecase/             # UseCase classes (business logic orchestration)
  application/service/             # write-transactional services
  application/adaptor/             # read-only transactional adaptor (@Adaptor, readOnly = true)
  domain/                          # Entities, domain services, value objects
  infrastructure/persistence/      # JPA repositories (extends JpaRepository)
  infrastructure/redis/            # Redis repositories and services
```

Business logic lives in UseCase classes, not in controllers or entities. Controllers delegate immediately to UseCases.

### UseCase Rules

- Annotate with `@UseCase` (meta-annotation of `@Component`)
- The public entry method is always named **`execute()`**
- Read-only UseCases: `@Transactional(readOnly = true)` at class level
- Write UseCases: `@Transactional` at class level
- UseCases orchestrate Adaptors and Services — Adaptors for reads, Services for writes

### Adaptor / Service Rules

- **Adaptor** (`@Adaptor`): `@Transactional(readOnly = true)` — wraps repository read queries, throws domain-specific exceptions on not-found
- **Service** (`@Service`, `@Transactional`): handles write operations (save, delete, update)
- JPA repositories live in `infrastructure/persistence/` and extend `JpaRepository`

### Member Profile Lookup

Use `MemberProfileRedisAdaptor.findProfile(memberId)` to get a member's nickname and `markerImageUrl`. This is a Redis cache-through — cache hit avoids DB, cache miss fetches with `JOIN FETCH equippedMarkerImage`. Prefer this over direct DB queries for member profile data.

### Key Modules

- **game** — Single-player road view games (practice + rank modes)
- **multi** — Multiplayer rooms with real-time WebSocket coordination
- **gamerank** — 8-tier ranking system
- **member / auth** — User profiles, OAuth2 (Google, Naver, Kakao), JWT
- **item / memberitem / point** — Shop economy and inventory
- **chat** — Real-time chat via Redis Streams (consumer groups)
- **notification** — Push notifications over WebSocket
- **friend** — Friend requests and real-time friend chat
- **coordinate** — Landmark location data
- **image** — S3-backed image management
- **common** — Shared infrastructure (security, caching, WebSocket, locking, S3, AOP)

### Infrastructure Patterns

- **Distributed locks**: Redisson for concurrency control in game room state changes (see `LockStrategyConfig`)
- **Async processing**: `@Async` for game statistics and notifications so game completion returns fast
- **Domain events**: Used for rank updates (`RoadViewRankEvent`) and practice completion (`RoadViewPracticeEvent`)
- **Redis**: Game room state, chat streams, distributed locks, session caching
- **WebSocket/STOMP**: Multiplayer coordination and notifications; SockJS fallback enabled

### API Response Format

All REST responses use a standard wrapper:
```json
{"isSuccess": true, "code": "...", "message": "...", "data": {...}}
```

### Security

Spring Security with JWT bearer tokens. OAuth2 login redirects issue JWTs. Some endpoints allow anonymous access (practice games, rankings, shop browsing).

## Configuration

- **Local**: `application-local.yml` — MySQL at `localhost:3306/kospot`, Redis at `localhost:6379`, hardcoded test credentials
- **Test**: `application-test.yml` — MySQL `kospot-test` DB with `create-drop` DDL, auto-rollback via transactions
- **Production secrets**: `.env` file (NOT in repo, lives in private submodule `KoSpot-backend-private`)

Activate profiles with `spring.profiles.active=local` or `test`.

## Testing

Tests are integration-style with a real MySQL (`kospot-test`) and Redis. Key test types:
- **UseCase tests**: Full Spring context, real DB/Redis
- **Concurrency tests**: Validate distributed lock correctness (e.g., `JoinGameRoomConcurrencyTest`)
- **WebSocket tests**: STOMP message handling

The test profile uses `create-drop` DDL — the schema is rebuilt each test run.
