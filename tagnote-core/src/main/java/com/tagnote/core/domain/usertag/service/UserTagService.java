package com.tagnote.core.domain.usertag.service;

import com.tagnote.core.domain.usertag.UserTagEntity;
import com.tagnote.core.domain.usertag.repository.UserTagRepository;
import com.tagnote.core.domain.usertag.name.NormalizedUserTagName;
import com.tagnote.core.domain.usertag.name.UserTagNameNormalizer;
import com.tagnote.core.domain.user.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserTagService {
    private final UserTagRepository userTagRepository;
    private final UserTagNameNormalizer userTagNameNormalizer;

    /**
     *  유저 태그 이름 리스트로 UserTagEntity의 리스트를 생성한다.
     */
    public List<UserTagEntity> makeUserTagList(UserEntity owner, List<String> userTagNameList) {
        Map<String, String> displayNameByNormalizedName = new LinkedHashMap<>();
        for (String rawName : userTagNameList) {
            String normalizedName = userTagNameNormalizer.normalize(rawName).value();
            displayNameByNormalizedName.putIfAbsent(normalizedName, rawName);
        }

        if (displayNameByNormalizedName.isEmpty()) {
            return List.of();
        }

        List<String> normalizedNames = List.copyOf(displayNameByNormalizedName.keySet());
        Map<String, UserTagEntity> userTagByNormalizedName = userTagRepository
                .findAllByOwner_UserIdAndNormalizedNameIn(owner.getUserId(), normalizedNames)
                .stream()
                .collect(Collectors.toMap(UserTagEntity::getNormalizedName, Function.identity()));

        List<UserTagEntity> newUserTags = displayNameByNormalizedName.entrySet().stream()
                .filter(entry -> !userTagByNormalizedName.containsKey(entry.getKey()))
                .map(entry -> UserTagEntity.create(owner, entry.getValue(), entry.getKey()))
                .toList();
        if (!newUserTags.isEmpty()) {
            userTagRepository.saveAllAndFlush(newUserTags);
            newUserTags.forEach(userTag -> userTagByNormalizedName.put(userTag.getNormalizedName(), userTag));
        }

        return normalizedNames.stream().map(userTagByNormalizedName::get).toList();
    }

    public NormalizedUserTagName normalize(String userTagName) {
        return userTagNameNormalizer.normalize(userTagName);
    }
}
