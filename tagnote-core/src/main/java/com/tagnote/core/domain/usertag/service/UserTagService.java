package com.tagnote.core.domain.usertag.service;

import com.tagnote.core.domain.usertag.UserTagEntity;
import com.tagnote.core.domain.usertag.repository.UserTagRepository;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserTagService {
    private final UserTagRepository userTagRepository;

    /**
     *  유저 태그 이름 리스트로 UserTagEntity의 리스트를 생성한다.
     */
    public List<UserTagEntity> makeUserTagList(List<String> userTagNameList) {
        List<UserTagEntity> existingUserTagList = userTagRepository.findAllByNameIn(userTagNameList);

        List<UserTagEntity> newUserTagList = userTagNameList.stream()
                .filter(name -> existingUserTagList.stream().noneMatch(userTag -> userTag.getName().equals(name)))
                .map(UserTagEntity::create)
                .toList();

        return Stream
                .concat(existingUserTagList.stream(), newUserTagList.stream())
                .toList();
    }

    /**
     * 유저 태그 이름으로 유저 태그 아이디를 조회한다.
     */
    public Long getUserTagIdByUserTagName(String userTagName) {
        return getUserTagByName(userTagName).getUserTagId();
    }


    /*
     * private
     */
    private UserTag getUserTagByName(String userTagName) {
        return userTagRepository.findByName(userTagName).orElseThrow(() -> new CustomException(ExceptionCode.USER_TAG_NOT_FOUND)).toUserTag();
    }
}
