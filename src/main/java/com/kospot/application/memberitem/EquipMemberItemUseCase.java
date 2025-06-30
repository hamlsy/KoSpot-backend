package com.kospot.application.memberitem;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.memberitem.service.MemberItemService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
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
