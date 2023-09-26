package com.tagstory.core.domain.hashtag.service;

import com.tagstory.core.domain.hashtag.HashtagEntity;
import com.tagstory.core.domain.hashtag.repository.HashtagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class HashtagService {
    private final HashtagRepository hashtagRepository;

    public List<HashtagEntity> makeHashtagList(List<String> hashtagStrList) {
        return hashtagStrList.stream()
                .map(hashtagStr -> hashtagRepository.findByName(hashtagStr)
                        .orElseGet(() -> hashtagRepository.save(HashtagEntity.create(hashtagStr))))
                .collect(Collectors.toList());
    }
}
