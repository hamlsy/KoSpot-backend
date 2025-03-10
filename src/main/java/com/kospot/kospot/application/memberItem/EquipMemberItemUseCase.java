package com.kospot.kospot.application.memberItem;

import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.memberItem.service.MemberItemService;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@Transactional
public class EquipMemberItemUseCase {

    private final MemberItemService memberItemService;

    public void execute(Member member, Long memberItemId){
        memberItemService.equipItem(member, memberItemId);
    }

}
