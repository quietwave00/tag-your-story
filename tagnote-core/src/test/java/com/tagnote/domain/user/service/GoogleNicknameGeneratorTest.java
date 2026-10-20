package com.tagnote.domain.user.service;

import com.tagnote.core.domain.user.service.GoogleNicknameGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleNicknameGeneratorTest {
    @Test
    void generated_nickname_uses_adjective_animal_and_four_to_six_digits() {
        GoogleNicknameGenerator generator = new GoogleNicknameGenerator();
        for (int i = 0; i < 100; i++) {
            String nickname = generator.generate();
            assertThat(nickname).matches("(푸른|밝은|고요한|작은|빛나는|자유로운)"
                    + "(여우|고양이|수달|참새|고래|토끼)[0-9]{4,6}");
            assertThat(nickname.length()).isBetween(1, 20);
        }
    }
}
