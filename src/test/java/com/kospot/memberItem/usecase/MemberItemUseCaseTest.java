package com.kospot.memberItem.usecase;

import com.kospot.application.item.FindAllItemsByTypeUseCase;
import com.kospot.application.memberItem.EquipMemberItemUseCase;
import com.kospot.application.memberItem.FindAllMemberItemsByItemTypeUseCase;
import com.kospot.application.memberItem.PurchaseItemUseCase;
import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.repository.ImageRepository;
import com.kospot.domain.item.entity.Item;
import com.kospot.domain.item.vo.ItemType;
import com.kospot.domain.item.repository.ItemRepository;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.domain.memberItem.adaptor.MemberItemAdaptor;
import com.kospot.domain.memberItem.entity.MemberItem;
import com.kospot.domain.memberItem.repository.MemberItemRepository;
import com.kospot.domain.memberItem.service.MemberItemService;
import com.kospot.presentation.item.dto.response.ItemResponse;
import com.kospot.presentation.memberItem.dto.response.MemberItemResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    @Autowired
    private FindAllItemsByTypeUseCase findAllItemsByTypeUseCase;

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

    @Autowired
    private ImageRepository imageRepository;

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
            Image image = Image.builder()
                    .imageUrl("" + i)
                    .build();
            imageRepository.save(image);
            Item item = Item.builder()
                    .name("item" + (i + 1))
                    .description("")
                    .itemType(ItemType.MARKER)
                    .price(50)
                    .stock(50)
                    .isAvailable(true)
                    .image(image)
                    .build();

            itemRepository.save(item);
        }
        for (int i = 5; i < 10; i++) {
            Image image = Image.builder()
                    .imageUrl("" + i)
                    .build();
            imageRepository.save(image);
            ItemType itemType = i == 6 ? ItemType.NONE : ItemType.MARKER;
            Item item = Item.builder()
                    .name("item" + (i + 1))
                    .description("")
                    .itemType(itemType)
                    .price(50)
                    .stock(50)
                    .isAvailable(true)
                    .image(image)
                    .build();
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
        logStart();
        Long itemId = 3L;

        //when
        logWhen();
        purchaseItemUseCase.execute(member, itemId);

        //then
        logThen();
        MemberItem memberItem = memberItemAdaptor.queryByItemIdFetchItem(itemId);
        assertNotNull(memberItem);
        assertEquals("item3", memberItem.getItem().getName());

    }

    @DisplayName("아이템 장착을 테스트합니다.")
    @Test
    void equipMemberItemUseCaseTest() {
        //given
        logStart();
        memberItemService.equipItem(member, 1L);
        memberItemService.equipItem(member, 2L);

        //when
        logWhen();
        equipMemberItemUseCase.execute(member, 3L);

        //then
        logThen();
        MemberItem memberItem1 = memberItemRepository.findByIdFetchItem(1L).orElseThrow();
        MemberItem memberItem2 = memberItemRepository.findById(3L).orElseThrow();
        assertEquals(true, memberItem2.getIsEquipped());
        assertEquals(false, memberItem1.getIsEquipped());

    }

    @DisplayName("내 아이템 조회를 테스트합니다.")
    @Test
    void findAllMemberItemsUseCaseTest() {
        //given

        //when
        logWhen();
        List<MemberItemResponse> response1 =
                findAllMemberItemsByItemTypeUseCase.execute(member, "marker");

        List<MemberItemResponse> response2 =
                findAllMemberItemsByItemTypeUseCase.execute(member, "none");

        //then
        log.info("response dto list: {} ", response1);
        log.info("response dto list: {} ", response2);
    }

    @DisplayName("아이템 조회를 테스트합니다.")
    @Test
    void findAllItemsByTypeUseCaseTest() {
        //given

        //when
        logWhen();
        List<ItemResponse> response = findAllItemsByTypeUseCase.execute(member, "marker");

        //then
        log.info("responses: {}", response);
    }

    private void logWhen() {
        log.info("-----when-----");
    }

    private void logThen() {
        log.info("-----then-----");
    }

    private void logStart() {
        log.info("-----start-----");
    }

}
