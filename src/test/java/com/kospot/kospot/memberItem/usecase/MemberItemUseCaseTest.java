package com.kospot.kospot.memberItem.usecase;

import com.kospot.kospot.application.memberItem.PurchaseItemUseCase;
import com.kospot.kospot.domain.item.entity.Item;
import com.kospot.kospot.domain.item.entity.ItemType;
import com.kospot.kospot.domain.item.repository.ItemRepository;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.member.repository.MemberRepository;
import com.kospot.kospot.domain.memberItem.adaptor.MemberItemAdaptor;
import com.kospot.kospot.domain.memberItem.entity.MemberItem;
import com.kospot.kospot.domain.memberItem.repository.MemberItemRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class MemberItemUseCaseTest {

    @Autowired
    private PurchaseItemUseCase purchaseItemUseCase;


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

    private Member member;

    @BeforeEach
    void setUp() {
        this.member = memberRepository.save(Member.builder()
                .username("user1")
                .nickname("nick1")
                .point(60)
                .build());

        for (int i = 0; i < 10; i++) {
            Item item = Item.create(
                    "item" + (i+1), "", ItemType.MARKER, 50, 50
            );

            itemRepository.save(item);
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

}
