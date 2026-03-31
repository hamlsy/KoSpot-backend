# Friend Feature Implementation Plan

## Objective
Implement friend capabilities in KoSpot backend with Spring clean architecture conventions:
1) my friends list with summary fields (`nickname`, `equippedMarkerImageUrl`, `isOnline`, `roadViewRank`, `ratingScore`)
2) per-friend 1:1 chat room and chat message persistence (DB-first)
3) friend delete
4) friend request create + receiver notification
5) friend request approve

Also deliver query optimization and Redis cache-aside strategy with deterministic invalidation.

## Scope
### In Scope
- New friend bounded context (`domain/application/presentation`)
- Friend request lifecycle (`PENDING`, `APPROVED`, `REJECTED`, `CANCELED`, `EXPIRED`)
- Friend relationship lifecycle (`ACTIVE`, `DELETED` soft-state)
- Friend list query optimization and cache
- Friend chat room + chat message persistence in DB
- Notification integration via existing friend request event/listener pipeline

### Out of Scope
- Group chat
- Attachments/images/files in friend chat
- Read receipts, typing indicators, push retry queue
- Redis stream or DB batch insert migration (future phase only)
- Social recommendation/mutual friends

## Existing Patterns To Reuse
- Layering and annotations from `README.md` and existing modules
- Member summary query composition pattern from `src/main/java/com/kospot/application/member/GetPlayerSummaryUseCase.java`
- Fetch-join/query annotation patterns from repositories like `src/main/java/com/kospot/domain/member/repository/MemberRepository.java`
- Event-driven notification pattern:
  - `src/main/java/com/kospot/domain/notification/event/FriendRequestCreatedEvent.java`
  - `src/main/java/com/kospot/application/notification/listener/FriendRequestNotificationEventListener.java`
- Redis config/key conventions:
  - `src/main/java/com/kospot/infrastructure/redis/common/config/RedisConfig.java`
  - `src/main/java/com/kospot/infrastructure/redis/common/constants/RedisKeyConstants.java`

## Architecture Decisions (Fixed)
1. **Friend Pair Canonicalization**: use ordered pair `(smallerMemberId, largerMemberId)` for both request and friendship uniqueness.
2. **Friend Request + Friendship Separation**:
   - `FriendRequest` tracks request workflow and audit.
   - `Friendship` tracks active relation and delete lifecycle.
3. **Idempotency Behavior**:
   - duplicate request to same pair returns existing pending request as success-like response.
   - duplicate approve on already approved request is treated as idempotent success.
   - delete non-existing friendship is idempotent success.
4. **Chat Boundary**: friend chat modeled separately from game-room chat to avoid coupling existing `ChatMessage.gameRoomId` schema.
5. **Notification Trigger**: publish existing `FriendRequestCreatedEvent` after successful friend request persistence in same transaction boundary.
6. **Consistency Model**:
   - DB is source of truth.
   - Redis cache-aside for read optimization with targeted evictions.
   - Redis outage fallback to DB reads with no feature hard-failure.

## Data Model Plan
### New Entities
- `FriendRequest`
  - `id`, `requesterMemberId`, `receiverMemberId`, `canonicalPairKey`, `status`, `expiresAt`, `actedAt`, `createdDate`, `modifiedDate`
- `Friendship`
  - `id`, `memberLowId`, `memberHighId`, `canonicalPairKey`, `status(ACTIVE/DELETED)`, `deletedAt`, `createdDate`, `modifiedDate`
- `FriendChatRoom`
  - `id`, `canonicalPairKey`, `memberLowId`, `memberHighId`, `lastMessageAt`, `createdDate`, `modifiedDate`
- `FriendChatMessage`
  - `id`, `messageId(UUID string)`, `roomId`, `senderMemberId`, `content`, `messageType(TEXT)`, `createdDate`

### Index/Constraint Plan
- `friend_request`: unique active pending request per canonical pair
- `friendship`: unique canonical pair
- `friend_chat_room`: unique canonical pair
- `friend_chat_message`: index `(room_id, created_date desc)`
- all tables: index by member id fields used for list lookup

## API Contract Plan
- `GET /friends` : my friend list (cursor/page)
- `POST /friends/requests` : send request
- `PATCH /friends/requests/{requestId}/approve` : approve request
- `PATCH /friends/requests/{requestId}/reject` : reject request
- `DELETE /friends/{friendMemberId}` : delete friend
- `GET /friends/requests/incoming` : incoming requests list
- `GET /friends/{friendMemberId}/chat-room` : get-or-create friend chat room id
- `GET /friends/chat-rooms/{roomId}/messages` : paged message list
- `POST /friends/chat-rooms/{roomId}/messages` : store message (DB-first)

## Redis Cache Plan
- Key namespace additions (under existing constants strategy):
  - `friend:list:{memberId}:v1`
  - `friend:summary:{memberId}:v1`
  - `friend:incoming:{memberId}:v1`
  - optional presence reuse from websocket/session infra for `isOnline`
- TTL:
  - friend list: 5 min
  - friend summary: 10 min
  - incoming requests: 2 min
- Invalidation map:
  - on request create: evict receiver incoming + both summaries
  - on approve/reject/cancel/expire: evict both users list/incoming/summaries
  - on friend delete: evict both users list/summaries and chat room cache metadata

## Performance/Query Plan
- Friend list endpoint must not perform per-row lookups for rank/marker/presence.
- Use projection query (single query + minimal secondary lookups if unavoidable).
- Add repository methods with explicit joins and/or DTO constructor projection.
- Validate query count with integration tests (baseline and target documented in tests).

## Security/Authz Plan
- All endpoints require authenticated member context.
- Member can only approve/reject requests where member is receiver.
- Member can only delete friendship where member is participant.
- Member can only access/send friend chat where member belongs to room participants.

## Task Execution Plan

### Task 1 - Friend Domain Package Scaffold
**Implementation**
- Create package tree:
  - `src/main/java/com/kospot/domain/friend/entity`
  - `src/main/java/com/kospot/domain/friend/repository`
  - `src/main/java/com/kospot/domain/friend/adaptor`
  - `src/main/java/com/kospot/domain/friend/service`
  - `src/main/java/com/kospot/domain/friend/vo`
  - `src/main/java/com/kospot/domain/friend/exception`
- Add baseline error status/handler aligned to existing domain exception patterns.
**QA Scenarios**
- Build compiles with new package wiring and no bean conflicts.
- Error mapping returns consistent response code format for friend-domain failures.

### Task 2 - Friend Request Entity + State Machine
**Implementation**
- Add `FriendRequest` entity with statuses: `PENDING`, `APPROVED`, `REJECTED`, `CANCELED`, `EXPIRED`.
- Add domain methods: `approveBy(receiverId)`, `rejectBy(receiverId)`, `cancelBy(requesterId)`, `expire(now)`.
- Add invariant checks (no self request, receiver-only approval/rejection, requester-only cancel).
**QA Scenarios**
- Illegal transition attempts throw domain exception.
- Receiver approves pending request -> status changes and `actedAt` populated.
- Approve/reject on non-pending request is blocked.

### Task 3 - Friendship Entity + Canonical Pair Key
**Implementation**
- Add `Friendship` entity with canonical pair (`memberLowId`, `memberHighId`, `canonicalPairKey`) and status (`ACTIVE`, `DELETED`).
- Add static helper for canonicalization from two member IDs.
- Add delete lifecycle method (`softDelete`) with idempotent no-op if already deleted.
**QA Scenarios**
- Pair key for `(A,B)` equals pair key for `(B,A)`.
- Duplicate creation attempt rejected by unique constraint path.
- Delete twice returns idempotent outcome.

### Task 4 - Friend Chat Room + Message Entities
**Implementation**
- Add `FriendChatRoom` (unique canonical pair) and `FriendChatMessage` (room-bound DB message) entities.
- Add `messageId` UUID dedup field and index `(roomId, createdDate desc)`.
- Keep model independent from existing game-room chat schema.
**QA Scenarios**
- One canonical pair yields exactly one room.
- Message pagination ordered by created timestamp descending.
- Duplicate messageId processing does not create duplicate rows.

### Task 5 - Repositories + Optimized Query Methods
**Implementation**
- Add repositories for friend request/friendship/chat room/chat message.
- Implement optimized friend list query method returning projection with required fields.
- Add request lookup methods for pending-by-pair, incoming-by-receiver, existing friendship-by-pair.
**QA Scenarios**
- Friend list query executes within expected query count (assertion test).
- Pending request lookup correctly handles both pair directions.
- Chat message page query returns deterministic, stable order.

### Task 6 - Adaptors for Read/Write Boundary
**Implementation**
- Add `FriendAdaptor` with query helpers and not-found/error translation.
- Add chat read adaptor methods for room ownership validation and message page retrieval.
- Reuse member adaptor for participant validation and profile/rank enrichment inputs.
**QA Scenarios**
- Unauthorized room/member access attempts are rejected.
- Missing request/friendship/room paths map to expected error status.
- Adaptor read methods marked read-only transactionally.

### Task 7 - UseCase: Send Friend Request
**Implementation**
- Add `SendFriendRequestUseCase` in `application/friend` with transactional write.
- Validate: not self, target member exists, no active friendship, no same pending request.
- Persist request and publish existing `FriendRequestCreatedEvent` after successful save.
- Define idempotent duplicate behavior: return existing pending request outcome.
**QA Scenarios**
- First request persists and emits one event.
- Duplicate request returns idempotent success without new row.
- Self-request returns validation failure.

### Task 8 - UseCase: Approve/Reject Friend Request
**Implementation**
- Add `ApproveFriendRequestUseCase` and `RejectFriendRequestUseCase`.
- Approve flow atomically updates request status and upserts `Friendship` as `ACTIVE`.
- Reject flow updates status and audit fields without friendship creation.
- Enforce receiver-only authorization.
**QA Scenarios**
- Approve creates friendship exactly once under concurrent retries.
- Reject leaves no friendship row.
- Non-receiver approval/rejection is forbidden.

### Task 9 - UseCase: Delete Friend + Incoming Request List
**Implementation**
- Add `DeleteFriendUseCase` (soft-delete active friendship by canonical pair).
- Add `GetIncomingFriendRequestsUseCase` with pagination and sender summary data.
- Delete endpoint is idempotent for non-existing or already-deleted relation.
**QA Scenarios**
- Delete active friendship removes it from subsequent friend list reads.
- Repeated delete returns idempotent success.
- Incoming request list shows only `PENDING` and correct receiver scope.

### Task 10 - UseCase: Friend List Query (Optimized)
**Implementation**
- Add `GetMyFriendsUseCase` returning required summary: nickname, marker image URL, online status, roadview rank, rating score.
- Compose summary using projection-first repository query and minimal enrichment passes.
- Source online status from existing websocket/session presence service if available; fallback false-safe default.
**QA Scenarios**
- Response includes all required fields for each friend.
- Query count test proves no N+1 explosion at list size >= 50.
- Performance test target met for p95 latency under fixture load.

### Task 11 - UseCase: Friend Chat Room and Message Persistence
**Implementation**
- Add `GetOrCreateFriendChatRoomUseCase`, `GetFriendChatMessagesUseCase`, `SendFriendChatMessageUseCase`.
- Guarantee one room per canonical pair.
- Persist messages in DB now; keep service interface seam for future Redis/batch pipeline.
**QA Scenarios**
- Same pair always resolves same room.
- Only room participants can read/send.
- Message send persists and appears in next page query.

### Task 12 - Presentation Layer (Controller + DTO)
**Implementation**
- Add `presentation/friend/controller/FriendController`.
- Add request/response DTOs for all friend endpoints.
- Use existing `ApiResponseDto` and Swagger annotation style.
**QA Scenarios**
- Contract tests verify status codes and response schema.
- Authenticated member context required for all endpoints.
- Invalid payloads produce consistent validation errors.

### Task 13 - Redis Cache Layer for Friend Reads
**Implementation**
- Add Redis key constants for friend list/summary/incoming requests.
- Implement cache-aside wrappers (friend list and incoming list first).
- Apply TTLs from plan and cache-version suffix (`:v1`).
**QA Scenarios**
- Cache miss -> DB read -> cache populate verified.
- Cache hit path bypasses DB repository call.
- Expired key naturally repopulates on next read.

### Task 14 - Deterministic Cache Invalidation
**Implementation**
- Add invalidation hooks in send/approve/reject/delete friend use cases.
- Evict exact keys for both participants according to invalidation map.
- Avoid wildcard broad deletes.
**QA Scenarios**
- After approve/delete, stale friend list is not returned.
- Incoming request cache reflects new state immediately after mutation.
- Redis unavailable during eviction does not break DB commit path.

### Task 15 - Notification Integration Verification
**Implementation**
- Keep publishing existing `FriendRequestCreatedEvent` from request use case.
- Verify compatibility with `FriendRequestNotificationEventListener` payload and `NotificationType.FRIEND_REQUEST`.
- Add integration test that request creation produces notification for receiver.
**QA Scenarios**
- Exactly one notification is generated for one successful request.
- Duplicate/idempotent request does not produce duplicate notifications.
- Receiver can fetch notification via existing notifications API.

### Task 16 - Concurrency and Idempotency Hardening
**Implementation**
- Add DB unique constraints + service-level duplicate handling.
- Add optimistic concurrency handling/retry strategy for approve vs delete race.
- Define deterministic API response for duplicate operations.
**QA Scenarios**
- Concurrent opposite-direction requests (`A->B` and `B->A`) settle into one pending canonical record policy.
- Concurrent duplicate approve yields one active friendship only.
- Concurrent delete/approve leaves consistent final state (no split-brain).

### Task 17 - Repository and Service Tests
**Implementation**
- Add unit tests for entity state machine and canonical pair utilities.
- Add repository integration tests for projections, pagination, and unique constraints.
- Add cache integration tests for hit/miss/invalidation.
**QA Scenarios**
- Illegal state transitions covered with explicit expected exceptions.
- Query result correctness verified for mixed rank/profile datasets.
- Redis fallback-to-DB behavior validated when cache operations fail.

### Task 18 - API Integration and Regression Tests
**Implementation**
- Add end-to-end test flow: request -> approve -> list -> chat -> delete.
- Add authorization tests (receiver-only approve/reject, participant-only chat access).
- Add backward-compatibility check ensuring existing chat/game/notification APIs unaffected.
**QA Scenarios**
- Full user journey passes with deterministic assertions.
- Unauthorized operations return expected forbidden status.
- Existing non-friend endpoints remain green.



## Final Verification Wave
- Run full friend module unit/integration tests.
- Run targeted repository query-count tests for friend list and chat pagination.
- Run API smoke tests for all new endpoints.
- Verify Redis down scenario fallback.
- Verify duplicate request/approve/delete idempotent behavior.

## Rollout / Future Hook
- Keep friend chat write path behind service interface to allow future Redis stream + batch insert migration.
- Document migration hooks in service-level TODO/ADR note.
