package com.tagstory.core.domain.hashtag.service;

import com.tagstory.core.domain.hashtag.HashtagEntity;
import com.tagstory.core.domain.hashtag.repository.HashtagRepository;
import com.tagstory.core.domain.hashtag.service.dto.Hashtag;
import com.tagstory.core.exception.CustomException;
import com.tagstory.core.exception.ExceptionCode;
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

    public List<HashtagEntity> makeHashtagList(List<String> hashtagStrList) {
        return hashtagStrList.stream()
            .map(hashtagStr -> hashtagRepository.findByName(hashtagStr)
            .orElseGet(() -> HashtagEntity.create(hashtagStr))).collect(Collectors.toList());
    }

    public Long getHashtagIdByHashtagName(String hashtagName) {
        return getHashtagByName(hashtagName).getHashtagId();
    }

    /*
     * 단일 메소드
     */
    private Hashtag getHashtagByName(String hashtagName) {
        return hashtagRepository.findByName(hashtagName).orElseThrow(() -> new CustomException(ExceptionCode.HASHTAG_NOT_FOUND)).toHashtag();
    }
}