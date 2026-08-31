package com.tagnote.domain.usertag.service;

import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.usertag.UserTagEntity;
import com.tagnote.core.domain.usertag.name.UserTagNameNormalizer;
import com.tagnote.core.domain.usertag.repository.UserTagRepository;
import com.tagnote.core.domain.usertag.service.UserTagService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserTagServiceTest {

    @Mock
    private UserTagRepository userTagRepository;

    @Spy
    private final UserTagNameNormalizer userTagNameNormalizer = new UserTagNameNormalizer();

    @InjectMocks
    private UserTagService userTagService;

    @Test
    void owner와_normalized_name목록으로_한번에_조회하고_요청중복을_제거한다() {
        UserEntity owner = UserEntity.builder().userId(1L).build();
        when(userTagRepository.findAllByOwner_UserIdAndNormalizedNameIn(1L, List.of("jazz", "r&b")))
                .thenReturn(List.of());

        List<UserTagEntity> result = userTagService.makeUserTagList(
                owner, List.of("Jazz", "  ＪＡＺＺ  ", "R&B")
        );

        assertThat(result).extracting(UserTagEntity::getName).containsExactly("Jazz", "R&B");
        assertThat(result).extracting(UserTagEntity::getNormalizedName).containsExactly("jazz", "r&b");
        ArgumentCaptor<List<UserTagEntity>> saved = ArgumentCaptor.forClass(List.class);
        verify(userTagRepository).saveAllAndFlush(saved.capture());
        assertThat(saved.getValue()).hasSize(2);
    }

    @Test
    void 같은_owner의_기존_identity를_재사용한다() {
        UserEntity owner = UserEntity.builder().userId(1L).build();
        UserTagEntity existing = UserTagEntity.builder()
                .userTagId(10L)
                .owner(owner)
                .name("Jazz")
                .normalizedName("jazz")
                .build();
        when(userTagRepository.findAllByOwner_UserIdAndNormalizedNameIn(1L, List.of("jazz")))
                .thenReturn(List.of(existing));

        List<UserTagEntity> result = userTagService.makeUserTagList(owner, List.of("  ＪＡＺＺ  "));

        assertThat(result).containsExactly(existing);
        verify(userTagRepository, never()).saveAllAndFlush(anyList());
    }
}
