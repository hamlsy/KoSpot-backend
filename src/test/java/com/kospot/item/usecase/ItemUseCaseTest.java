package com.kospot.item.usecase;

import com.kospot.application.item.*;
import com.kospot.kospot.application.item.*;
import com.kospot.domain.item.adaptor.ItemAdaptor;
import com.kospot.domain.item.entity.Item;
import com.kospot.domain.item.entity.ItemType;
import com.kospot.domain.item.repository.ItemRepository;
import com.kospot.domain.item.service.ItemService;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.entity.Role;
import com.kospot.domain.member.repository.MemberRepository;
import com.kospot.presentation.item.dto.request.ItemRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@Slf4j
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class ItemUseCaseTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemService itemService;

    @Autowired
    private DeleteItemFromShopUseCase deleteItemFromShopUseCase;

    @Autowired
    private DeleteItemUseCase deleteItemUseCase;

    @Autowired
    private FindAllItemsByTypeUseCase findAllItemsByTypeUseCase;

    @Autowired
    private RegisterItemUseCase registerItemUseCase;

    @Autowired
    private UpdateItemInfoUseCase updateItemInfoUseCase;

    @Autowired
    private ItemAdaptor itemAdaptor;


    private Member adminMember;

    private Member member;

    @BeforeEach
    void setUp() {
        this.adminMember = Member.builder()
                .username("admin1")
                .nickname("admin1")
                .role(Role.ADMIN)
                .build();

        this.member = Member.builder()
                .username("member1")
                .nickname("member1")
                .role(Role.USER)
                .build();

        memberRepository.save(adminMember);
        memberRepository.save(member);
    }

    @DisplayName("아이템 등록 테스트")
    @Test
    void registerItemUseCaseTest() {
        //given
        ItemRequest.Create request = ItemRequest.Create.builder()
                .name("name1")
                .itemTypeKey("marker")
                .image(mock(MultipartFile.class))
                .build();

        //when
        // not admin
        assertThrows(Exception.class, () -> registerItemUseCase.execute(member, request));
        itemService.registerItem(request);

        //then
        Item item = itemRepository.findById(1L).orElseThrow();
        assertEquals("name1", item.getName());

    }

    @DisplayName("아이템 수정 테스트")
    @Test
    void updateItemInfoUseCaseTest() {
        //given
        Item item = itemRepository.save(Item.builder()
                .name("name2")
                .build());
        ItemRequest.UpdateInfo request = ItemRequest.UpdateInfo.builder()
                .itemId(item.getId())
                .itemTypeKey("markEr")
                .name("updatedName2")
                .build();

        //when
        //not admin
        assertThrows(Exception.class, () -> updateItemInfoUseCase.execute(member, request));
        updateItemInfoUseCase.execute(adminMember, request);

        //then
        Item updatedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertEquals("updatedName2", updatedItem.getName());

    }

    @DisplayName("아이템 상점 삭제 테스트")
    @Test
    void deleteItemFromShopUseCaseTest() {
        //given
        Item item = itemRepository.save(Item.builder()
                .name("name3")
                .itemType(ItemType.MARKER)
                .isAvailable(true)
                .build());
        //when
        //not admin
        assertThrows(Exception.class, () -> deleteItemFromShopUseCase.execute(member, item.getId()));
        deleteItemFromShopUseCase.execute(adminMember, item.getId());

        //then
        List<Item> itemList = itemAdaptor.queryAvailableItemsByItemTypeFetchImage(ItemType.MARKER);
        boolean isEmpty = itemList.isEmpty();
        assertTrue(isEmpty);

    }

    @DisplayName("아이템 삭제 테스트")
    @Test
    void deleteItemUseCaseTest() {
        //given
        Item item = itemRepository.save(Item.builder()
                .name("name4")
                .build());
        //todo add memberItem

        //when
        //not admin
        assertThrows(Exception.class, () -> deleteItemUseCase.execute(member, item.getId()));
        deleteItemUseCase.execute(adminMember, item.getId());

        //then
        assertThrows(Exception.class, () -> itemRepository.findById(item.getId()).orElseThrow());

    }

}
