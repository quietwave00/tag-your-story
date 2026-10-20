package com.tagnote.core.domain.user.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class GoogleNicknameGenerator {
    private static final String[] ADJECTIVES = {"푸른", "밝은", "고요한", "작은", "빛나는", "자유로운"};
    private static final String[] ANIMALS = {"여우", "고양이", "수달", "참새", "고래", "토끼"};
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        int[] minimums = {1000, 10000, 100000};
        int minimum = minimums[random.nextInt(minimums.length)];
        String nickname = ADJECTIVES[random.nextInt(ADJECTIVES.length)]
                + ANIMALS[random.nextInt(ANIMALS.length)]
                + (minimum + random.nextInt(9 * minimum));
        if (nickname.length() > 20) {
            throw new IllegalStateException("Generated nickname exceeds limit");
        }
        return nickname;
    }
}
