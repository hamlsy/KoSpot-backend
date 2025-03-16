package com.kospot.kospot.memberItem.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.kospot.application.memberItem.EquipMemberItemUseCase;
import com.kospot.kospot.application.memberItem.FindAllMemberItemsByItemTypeUseCase;
import com.kospot.kospot.application.memberItem.PurchaseItemUseCase;
import com.kospot.kospot.domain.item.entity.Item;
import com.kospot.kospot.domain.item.entity.ItemType;
import com.kospot.kospot.domain.item.repository.ItemRepository;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.member.repository.MemberRepository;
import com.kospot.kospot.domain.memberItem.adaptor.MemberItemAdaptor;
import com.kospot.kospot.domain.memberItem.entity.MemberItem;
import com.kospot.kospot.domain.memberItem.repository.MemberItemRepository;
import com.kospot.kospot.domain.memberItem.service.MemberItemService;
import com.kospot.kospot.presentation.memberItem.dto.response.MemberItemResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class MemberItemUseCaseTest {

    @Autowired
    private PurchaseItemUseCase purchaseItemUseCase;

    @Autowired
    private EquipMemberItemUseCase equipMemberItemUseCase;

    @Autowired
    private FindAllMemberItemsByItemTypeUseCase findAllMemberItemsByItemTypeUseCase;

    //adaptor
    @Autowired
    private MemberItemAdaptor memberItemAdaptor;

    // repository
    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberItemRepository memberItemRepository;

    //service
    @Autowired
    private MemberItemService memberItemService;

    private Member member;

    @BeforeEach
    void setUp() {
        this.member = memberRepository.save(Member.builder()
                .username("user1")
                .nickname("nick1")
                .point(99999)
                .build());

        for (int i = 0; i < 5; i++) {
            Item item = Item.create(
                    "item" + (i + 1), "", ItemType.MARKER, 50, 50
            );

            itemRepository.save(item);
        }
        for (int i = 5; i < 10; i++) {
            ItemType itemType = i == 6 ? ItemType.NONE : ItemType.MARKER;
            Item item = Item.create(
                    "item" + (i + 1), "", itemType, 50, 50
            );

            itemRepository.save(item);
            MemberItem memberItem = MemberItem.builder()
                    .item(item)
                    .isEquipped(false)
                    .member(member)
                    .build();
            memberItemRepository.save(memberItem);
        }

    }

    @DisplayName("아이템 구매를 테스트합니다.")
    @Test
    void purchaseItemUseCaseTest() {
        //given
        log.info("-----test start------");
        Long itemId = 3L;

        //when
        purchaseItemUseCase.execute(member, itemId);

        //then
        log.info("query by item id");
        MemberItem memberItem = memberItemAdaptor.queryByItemIdFetchItem(itemId);
        assertNotNull(memberItem);
        assertEquals("item3", memberItem.getItem().getName());

    }

    @DisplayName("아이템 장착을 테스트합니다.")
    @Test
    void equipMemberItemUseCaseTest() {
        //given
        log.info("-----test start------");
        memberItemService.equipItem(member, 1L);
        memberItemService.equipItem(member, 2L);

        //when
        log.info("-----when-----");
        equipMemberItemUseCase.execute(member, 3L);

        //then
        log.info("-----then------");
        MemberItem memberItem1 = memberItemRepository.findByIdFetchItem(1L).orElseThrow();
        MemberItem memberItem2 = memberItemRepository.findById(3L).orElseThrow();
        assertEquals(true, memberItem2.getIsEquipped());
        assertEquals(false, memberItem1.getIsEquipped());

    }

    @DisplayName("내 아이템 조회를 테스트합니다.")
    @Test
    void findAllMemberItemsUseCaseTest () {
        //given

        //when
        log.info("-----when-----");
        List<MemberItemResponse> response1 =
                findAllMemberItemsByItemTypeUseCase.execute(member, "marker");

        List<MemberItemResponse> response2 =
                findAllMemberItemsByItemTypeUseCase.execute(member, "none");

        //then
        log.info("response dto list: {} ", response1);
        log.info("response dto list: {} ", response2);
    }

}
