package com.kospot.presentation.friend.controller;

import com.kospot.application.friend.ApproveFriendRequestUseCase;
import com.kospot.application.friend.DeleteFriendUseCase;
import com.kospot.application.friend.GetFriendChatMessagesUseCase;
import com.kospot.application.friend.GetIncomingFriendRequestsUseCase;
import com.kospot.application.friend.GetMyFriendsUseCase;
import com.kospot.application.friend.GetOrCreateFriendChatRoomUseCase;
import com.kospot.application.friend.RejectFriendRequestUseCase;
import com.kospot.application.friend.SendFriendRequestUseCase;
import com.kospot.infrastructure.exception.payload.code.SuccessStatus;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import com.kospot.infrastructure.security.aop.CurrentMember;
import com.kospot.presentation.friend.dto.request.FriendRequestCreateRequest;
import com.kospot.presentation.friend.dto.response.FriendChatMessageResponse;
import com.kospot.presentation.friend.dto.response.FriendChatRoomResponse;
import com.kospot.presentation.friend.dto.response.FriendListResponse;
import com.kospot.presentation.friend.dto.response.FriendRequestActionResponse;
import com.kospot.presentation.friend.dto.response.IncomingFriendRequestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Friend Api", description = "친구 API")
@RequestMapping("/friends")
public class FriendController {

    private final GetMyFriendsUseCase getMyFriendsUseCase;
    private final SendFriendRequestUseCase sendFriendRequestUseCase;
    private final ApproveFriendRequestUseCase approveFriendRequestUseCase;
    private final RejectFriendRequestUseCase rejectFriendRequestUseCase;
    private final DeleteFriendUseCase deleteFriendUseCase;
    private final GetIncomingFriendRequestsUseCase getIncomingFriendRequestsUseCase;
    private final GetOrCreateFriendChatRoomUseCase getOrCreateFriendChatRoomUseCase;
    private final GetFriendChatMessagesUseCase getFriendChatMessagesUseCase;

    @Operation(summary = "내 친구 목록 조회", description = "내 친구들의 요약 정보를 조회합니다.")
    @GetMapping
    public ApiResponseDto<List<FriendListResponse>> getMyFriends(@CurrentMember Long memberId) {
        return ApiResponseDto.onSuccess(getMyFriendsUseCase.execute(memberId));
    }

    @Operation(summary = "친구 요청", description = "특정 회원에게 친구 요청을 보냅니다.")
    @PostMapping("/requests")
    public ApiResponseDto<FriendRequestActionResponse> sendFriendRequest(
            @CurrentMember Long memberId,
            @Valid @RequestBody FriendRequestCreateRequest request) {
        return ApiResponseDto.onSuccess(sendFriendRequestUseCase.execute(memberId, request.receiverMemberId()));
    }

    @Operation(summary = "친구 요청 승인", description = "친구 요청을 승인합니다.")
    @PatchMapping("/requests/{requestId}/approve")
    public ApiResponseDto<FriendRequestActionResponse> approveFriendRequest(
            @CurrentMember Long memberId,
            @PathVariable("requestId") Long requestId) {
        return ApiResponseDto.onSuccess(approveFriendRequestUseCase.execute(memberId, requestId));
    }

    @Operation(summary = "친구 요청 거절", description = "받은 친구 요청을 거절합니다.")
    @PatchMapping("/requests/{requestId}/reject")
    public ApiResponseDto<FriendRequestActionResponse> rejectFriendRequest(
            @CurrentMember Long memberId,
            @PathVariable("requestId") Long requestId) {
        return ApiResponseDto.onSuccess(rejectFriendRequestUseCase.execute(memberId, requestId));
    }

    @Operation(summary = "받은 친구 요청 조회", description = "내가 받은 친구 요청 목록을 조회합니다.")
    @GetMapping("/requests/incoming")
    public ApiResponseDto<List<IncomingFriendRequestResponse>> getIncomingRequests(
            @CurrentMember Long memberId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", required = false) Integer size) {
        return ApiResponseDto.onSuccess(getIncomingFriendRequestsUseCase.execute(memberId, page, size));
    }

    @Operation(summary = "친구 삭제", description = "친구 관계를 삭제합니다.")
    @DeleteMapping("/{friendMemberId}")
    public ApiResponseDto<?> deleteFriend(
            @CurrentMember Long memberId,
            @PathVariable("friendMemberId") Long friendMemberId) {
        deleteFriendUseCase.execute(memberId, friendMemberId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "친구 채팅방 조회/생성", description = "특정 친구와의 1:1 채팅방을 조회하거나 생성합니다.")
    @GetMapping("/{friendMemberId}/chat-room")
    public ApiResponseDto<FriendChatRoomResponse> getOrCreateChatRoom(
            @CurrentMember Long memberId,
            @PathVariable("friendMemberId") Long friendMemberId) {
        return ApiResponseDto.onSuccess(getOrCreateFriendChatRoomUseCase.execute(memberId, friendMemberId));
    }

    @Operation(summary = "친구 채팅 메시지 조회", description = "친구 채팅방의 메시지를 최신순으로 조회합니다.")
    @GetMapping("/chat-rooms/{roomId}/messages")
    public ApiResponseDto<List<FriendChatMessageResponse>> getChatMessages(
            @CurrentMember Long memberId,
            @PathVariable("roomId") Long roomId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", required = false) Integer size) {
        return ApiResponseDto.onSuccess(getFriendChatMessagesUseCase.execute(memberId, roomId, page, size));
    }
}
