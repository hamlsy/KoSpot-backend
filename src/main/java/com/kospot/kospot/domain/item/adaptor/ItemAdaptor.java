package com.kospot.kospot.domain.item.adaptor;

import com.kospot.kospot.global.annotation.adaptor.Adaptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Adaptor
@Transactional(readOnly = true)
public class ItemAdaptor {

}
