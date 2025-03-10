package com.kospot.kospot.application.item;

import com.kospot.kospot.domain.item.service.ItemService;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional
public class DeleteItemUseCase {

    private ItemService itemService;

    public void execute(Long id){


    }

}
