package com.tagnote.application.user;

import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.repository.UserRepository;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NicknameChangeServiceTest {
    @Mock NicknameChangeWriter writer;
    @Mock UserRepository users;

    private NicknameChangeService service() { return new NicknameChangeService(writer, users); }

    @Test
    void accepts_manual_nickname_and_invalidates_user_cache_after_commit() {
        when(writer.change(7L, "나의음악7")).thenReturn(UserEntity.builder()
                .userId(7L).userKey("key-7").nickname("나의음악7").build());

        assertThat(service().change(7L, "나의음악7")).isEqualTo("나의음악7");
        verify(users).deleteCachedUser(7L, "key-7");
    }

    @Test
    void rejects_invalid_input_before_writing() {
        for (String value : new String[]{"", " ", "a b", "a\t", "a\u00a0b", "가".repeat(21)}) {
            assertThatThrownBy(() -> service().change(7L, value))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getExceptionCode())
                            .isEqualTo(ExceptionCode.NICKNAME_INVALID));
        }
        assertThatThrownBy(() -> service().change(7L, null))
                .isInstanceOf(CustomException.class);
        verifyNoInteractions(writer, users);
    }

    @Test
    void maps_committed_nickname_unique_conflict_after_writer_transaction_rolls_back() {
        when(writer.change(7L, "taken")).thenThrow(new DataIntegrityViolationException("unique"));
        when(users.existsByNickname("taken")).thenReturn(true);
        assertThatThrownBy(() -> service().change(7L, "taken"))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getExceptionCode())
                        .isEqualTo(ExceptionCode.NICKNAME_ALREADY_EXISTS));
        verify(users, never()).deleteCachedUser(anyLong(), anyString());
    }

    @Test
    void unrelated_integrity_failure_is_not_reported_as_nickname_conflict() {
        when(writer.change(7L, "free")).thenThrow(new DataIntegrityViolationException("other"));
        assertThatThrownBy(() -> service().change(7L, "free"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
