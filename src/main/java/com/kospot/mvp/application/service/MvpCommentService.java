package com.kospot.mvp.application.service;

import com.kospot.mvp.domain.entity.MvpComment;
import com.kospot.mvp.infrastructure.persistence.MvpCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MvpCommentService {

    private final MvpCommentRepository mvpCommentRepository;

    public MvpComment save(MvpComment comment) {
        return mvpCommentRepository.save(comment);
    }

    public void delete(MvpComment comment) {
        mvpCommentRepository.delete(comment);
    }
}
