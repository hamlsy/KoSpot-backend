package com.kospot.presentation.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchMemberResponse {

    private Long memberId;
    private String nickname;
    private String markerImageUrl;
    private boolean isFriend;
    private boolean requestSend;

}
