package com.tagnote.core.domain.usertag.service;

import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.usertag.UserTagEntity;
import com.tagnote.core.domain.usertag.repository.UserTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserTagService {

    private final UserTagRepository userTagRepository;

    public List<UserTagEntity> makeUserTagList(UserEntity owner, List<String> userTagNameList) {
        LinkedHashSet<String> uniqueNames = new LinkedHashSet<>();
        for (String name : userTagNameList) {
            validateName(name);
            uniqueNames.add(name);
        }

        if (uniqueNames.isEmpty()) {
            return List.of();
        }

        List<String> names = List.copyOf(uniqueNames);
        Map<String, UserTagEntity> userTagByName = userTagRepository
                .findAllByOwner_UserIdAndNameIn(owner.getUserId(), names)
                .stream()
                .collect(Collectors.toMap(UserTagEntity::getName, Function.identity()));

        List<UserTagEntity> newUserTags = names.stream()
                .filter(name -> !userTagByName.containsKey(name))
                .map(name -> UserTagEntity.create(owner, name))
                .toList();
        if (!newUserTags.isEmpty()) {
            userTagRepository.saveAllAndFlush(newUserTags);
            newUserTags.forEach(userTag -> userTagByName.put(userTag.getName(), userTag));
        }

        return names.stream().map(userTagByName::get).toList();
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("User tag name must not be blank");
        }
    }
}
