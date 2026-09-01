package com.tagnote.domain.usertag.service;

import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.usertag.UserTagEntity;
import com.tagnote.core.domain.usertag.repository.UserTagRepository;
import com.tagnote.core.domain.usertag.service.UserTagService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserTagServiceTest {

    @Mock
    private UserTagRepository userTagRepository;

    @InjectMocks
    private UserTagService userTagService;

    @Test
    void owner와_입력_name목록으로_한번에_조회하고_정확히_같은_요청중복만_제거한다() {
        UserEntity owner = UserEntity.builder().userId(1L).build();
        when(userTagRepository.findAllByOwner_UserIdAndNameIn(
                1L, List.of("Jazz", "jazz", "  Jazz  ")
        )).thenReturn(List.of());

        List<UserTagEntity> result = userTagService.makeUserTagList(
                owner, List.of("Jazz", "Jazz", "jazz", "  Jazz  ")
        );

        assertThat(result).extracting(UserTagEntity::getName)
                .containsExactly("Jazz", "jazz", "  Jazz  ");
        ArgumentCaptor<List<UserTagEntity>> saved = ArgumentCaptor.forClass(List.class);
        verify(userTagRepository).saveAllAndFlush(saved.capture());
        assertThat(saved.getValue()).hasSize(3);
    }

    @Test
    void 같은_owner의_정확히_같은_name_identity를_재사용한다() {
        UserEntity owner = UserEntity.builder().userId(1L).build();
        UserTagEntity existing = UserTagEntity.builder()
                .userTagId(10L)
                .owner(owner)
                .name("1월")
                .build();
        when(userTagRepository.findAllByOwner_UserIdAndNameIn(1L, List.of("1월")))
                .thenReturn(List.of(existing));

        List<UserTagEntity> result = userTagService.makeUserTagList(owner, List.of("1월"));

        assertThat(result).containsExactly(existing);
        verify(userTagRepository, never()).saveAllAndFlush(anyList());
    }

    @Test
    void blank와_null_name은_거부하되_유효한_name은_변형하지_않는다() {
        UserEntity owner = UserEntity.builder().userId(1L).build();

        assertThatThrownBy(() -> userTagService.makeUserTagList(owner, List.of(" ")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userTagService.makeUserTagList(
                owner, java.util.Arrays.asList((String) null)
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
