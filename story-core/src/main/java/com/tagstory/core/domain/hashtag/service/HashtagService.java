package com.tagstory.core.domain.hashtag.service;

import com.tagstory.core.domain.hashtag.HashtagEntity;
import com.tagstory.core.domain.hashtag.repository.HashtagRepository;
import com.tagstory.core.exception.CustomException;
import com.tagstory.core.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HashtagService {
    private final HashtagRepository hashtagRepository;

    /**
     *  해시태그 이름 리스트로 HashtagEntity의 리스트를 생성한다.
     *
     * @param hashtagNameList
     * @return List<HashtagEntity>
     */
    public List<HashtagEntity> makeHashtagList(List<String> hashtagNameList) {
        List<HashtagEntity> existingHashtagList = hashtagRepository.findAllByNameIn(hashtagNameList);

        List<HashtagEntity> newHahstagList = hashtagNameList.stream()
                .filter(name -> existingHashtagList.stream().noneMatch(hashtag -> hashtag.getName().equals(name)))
                .map(HashtagEntity::create)
                .toList();

        return Stream
                .concat(existingHashtagList.stream(), newHahstagList.stream())
                .toList();
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
