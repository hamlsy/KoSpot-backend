package com.kospot.kospot.domain.item.service;

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
    private final AwsS3Service awsS3Service;

    //todo optimize image upload transaction
    // Item Create가 실패해도 S3에 이미지가 올라가는 문제 발생
    public void registerItem(Member member, ItemRequest.Create request) {
        member.validateAdmin();
        String imageUrl = awsS3Service.uploadImage(request.getImage());
        Item item = request.toEntity(imageUrl);

        itemRepository.save(item);
    }

    // test
    public void registerItemTest(ItemRequest.Create request) {
        String imageUrl = awsS3Service.uploadImage(request.getImage());
        Item item = request.toEntity(imageUrl);

        itemRepository.save(item);
    }

}
