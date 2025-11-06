package com.kospot.infrastructure.initializer.item;

import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.repository.ImageRepository;
import com.kospot.domain.image.vo.ImageType;
import com.kospot.domain.item.entity.Item;
import com.kospot.domain.item.repository.ItemRepository;
import com.kospot.domain.item.vo.ItemType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class DefaultItemInitializer implements CommandLineRunner {

    private final ItemRepository itemRepository;
    private final ImageRepository imageRepository;
    private final static String DEFAULT_MARKER_IMAGE_URL = "https://kospot-files.s3.ap-northeast-2.amazonaws.com/file/image/item/marker/default_marker.png";

    @Override
    public void run(String... args) throws Exception {
        if(itemRepository.count() > 0) {
            log.info("기본 아이템 이미 존재 - 초기화 스킵");
            return;
        }
        // 없을 경우만 초기 마커 데이터 삽입
        Image image = Image.create(
                "file/image/item/marker/",
                "default_marker.png",
                "file/image/item/marker/default_marker.png",
                DEFAULT_MARKER_IMAGE_URL,
                ImageType.ITEM
        );
        imageRepository.save(image);
        Item item = Item.createDefault("기본 마커", "기본으로 제공되는 마커입니다.", ItemType.MARKER, image);

        itemRepository.save(item);
    }

}
