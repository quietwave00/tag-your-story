package com.tagnote.domain.user;

import com.tagnote.core.domain.user.EmailNormalizer;
import com.tagnote.core.domain.user.AccountProvider;
import com.tagnote.core.domain.user.service.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailNormalizerTest {
    @Test
    void trim과_ROOT_소문자화가_동일한_identity를_만든다() {
        assertThat(EmailNormalizer.normalize(" User@Example.com ")).isEqualTo("user@example.com");
        assertThat(EmailNormalizer.normalize("user@example.com")).isEqualTo("user@example.com");
        assertThat(EmailNormalizer.normalize("a.b+tag@gmail.com"))
                .isNotEqualTo(EmailNormalizer.normalize("ab@gmail.com"));
    }

    @Test
    void 잘못된_형식을_거부한다() {
        for (String email : new String[]{"", "plain", "a@", "a @example.com", "a@@example.com"}) {
            assertThatThrownBy(() -> EmailNormalizer.normalize(email))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> EmailNormalizer.normalize(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 기존_Google_pending_등록은_key와_subject를_보존한다() {
        User pending = User.builder().userKey("google-sub")
                .email(" User@Example.com ").nickname("listener").build();

        var entity = pending.toEntity();
        assertThat(entity.getUserKey()).isEqualTo("google-sub");
        assertThat(entity.getSubject()).isEqualTo("google-sub");
        assertThat(entity.getProvider()).isEqualTo(AccountProvider.GOOGLE);
        assertThat(entity.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void 관계_참조용_user_id만_있는_객체는_email없이_Entity로_변환할_수_있다() {
        var reference = User.builder().userId(42L).build().toEntity();

        assertThat(reference.getUserId()).isEqualTo(42L);
        assertThat(reference.getEmail()).isNull();
    }
}
