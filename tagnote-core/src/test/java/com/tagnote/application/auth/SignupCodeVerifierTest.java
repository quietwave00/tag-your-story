package com.tagnote.application.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SignupCodeVerifierTest {
    @Test
    void verifierBindsCodeToChallengeAndSecret() {
        SignupCodeVerifier signer = new SignupCodeVerifier("0123456789abcdef0123456789abcdef");
        String first = signer.sign("code", "id-one:000001");
        assertThat(first).hasSize(64);
        assertThat(first).isNotEqualTo(signer.sign("code", "id-two:000001"));
        assertThat(first).isNotEqualTo(signer.sign("code", "id-one:000002"));
        assertThat(first).isNotEqualTo(new SignupCodeVerifier("abcdef0123456789abcdef0123456789")
                .sign("code", "id-one:000001"));
        assertThat(signer.matches(first, signer.sign("code", "id-one:000001"))).isTrue();
    }
}
