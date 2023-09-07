package com.tagstory.core.domain.hashtag.service;

import com.tagstory.core.domain.hashtag.HashtagEntity;
import com.tagstory.core.domain.hashtag.repository.HashtagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class HashtagService {
    private final HashtagRepository hashtagRepository;

    public List<HashtagEntity> getHashtagList(List<String> hashtagStrList) {
        return hashtagStrList.stream()
                .map(HashtagEntity::create)
                .collect(Collectors.toList());
    }
}
