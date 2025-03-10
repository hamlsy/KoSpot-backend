package com.kospot.kospot.domain.item.service;

import com.kospot.kospot.domain.item.adaptor.ItemAdaptor;
import com.kospot.kospot.domain.item.dto.request.ItemRequest;
import com.kospot.kospot.domain.item.entity.Item;

import com.kospot.kospot.domain.item.repository.ItemRepository;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.global.service.AwsS3Service;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemAdaptor itemAdaptor;
    private final AwsS3Service awsS3Service;

    //todo optimize image upload transaction
    // Item Create가 실패해도 S3에 이미지가 올라가는 문제 발생
    public void registerItem(Member member, ItemRequest.Create request) {
        member.validateAdmin();

        // todo item type에 따라 다르게
        String imageUrl = awsS3Service.uploadItemMarkerImage(request.getImage());
        Item item = request.toEntity(imageUrl);

        itemRepository.save(item);
    }

    // todo test code <- 삭제 예정
    public void registerItemTest(ItemRequest.Create request) {
        String imageUrl = awsS3Service.uploadItemMarkerImage(request.getImage());
        Item item = request.toEntity(imageUrl);

        itemRepository.save(item);
    }

    // todo item 완전 삭제와 상점에서의 삭제를 구분하기
    // todo 연관관계 고려, memberItem, member,
    public void deleteItemById(Long id){
        itemRepository.deleteById(id);
    }

}
