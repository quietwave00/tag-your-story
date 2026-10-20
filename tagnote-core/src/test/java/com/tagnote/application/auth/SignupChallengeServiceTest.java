package com.tagnote.application.auth;

import com.tagnote.application.auth.port.SignupChallengeStore;
import com.tagnote.application.auth.port.SignupMailSender;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SignupChallengeServiceTest {
    private final SignupChallengeStore store = mock(SignupChallengeStore.class);
    private final SignupMailSender sender = mock(SignupMailSender.class);
    private final SignupCodeVerifier signer = new SignupCodeVerifier("0123456789abcdef0123456789abcdef");
    private SignupChallengeService service;

    @BeforeEach
    void setUp() {
        service = new SignupChallengeService(store, sender, signer);
    }

    @Test
    void startStoresOnlyVerifierThenActivatesAfterMail() {
        when(store.reserveNew(anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(1);
        when(store.activate(anyString(), anyString())).thenReturn(true);

        String id = service.start("  USER@Example.com ", "$2a$hashed-password", "192.0.2.1");

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(sender).sendCode(eq("user@example.com"), code.capture());
        assertThat(code.getValue()).matches("[0-9]{6}");
        String verifier = signer.sign("code", id + ":" + code.getValue());
        verify(store).reserveNew(eq(id), eq("user@example.com"), eq("$2a$hashed-password"),
                eq(verifier), anyString(), anyString());
        verify(store).activate(id, verifier);
        var order = inOrder(store, sender);
        order.verify(store).reserveNew(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        order.verify(sender).sendCode(anyString(), anyString());
        order.verify(store).activate(anyString(), anyString());
    }

    @Test
    void mailFailureDiscardsReservationAndReturnsServiceError() {
        when(store.reserveNew(anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(1);
        doThrow(new IllegalStateException("smtp down")).when(sender).sendCode(anyString(), anyString());

        assertThatThrownBy(() -> service.start("user@example.com", "hash", "192.0.2.1"))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getExceptionCode()).isEqualTo(ExceptionCode.SIGNUP_MAIL_UNAVAILABLE));
        verify(store).discard(anyString(), anyString());
        verify(store, never()).activate(anyString(), anyString());
    }

    @Test
    void activationFailureIsNotReportedAsSuccess() {
        when(store.reserveNew(anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(1);
        when(store.activate(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.start("user@example.com", "hash", "192.0.2.1"))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getExceptionCode()).isEqualTo(ExceptionCode.SIGNUP_CHALLENGE_UNAVAILABLE));
        verify(store).discard(anyString(), anyString());
    }

    @Test
    void resendReplacesVerifierAndSendsOnlyAfterReservation() {
        when(store.verify(anyString(), isNull())).thenReturn(new SignupChallengeStore.Verification(
                SignupChallengeStore.Verification.Status.VALID, "user@example.com", "hash"));
        when(store.reserveResend(anyString(), anyString(), anyString(), anyString())).thenReturn(1);
        when(store.activate(anyString(), anyString())).thenReturn(true);

        service.resend("53e65cc8-8e8e-49eb-a309-c084566a8051", "192.0.2.1");

        var order = inOrder(store, sender);
        order.verify(store).reserveResend(anyString(), anyString(), anyString(), anyString());
        order.verify(sender).sendCode(eq("user@example.com"), matches("[0-9]{6}"));
        order.verify(store).activate(anyString(), anyString());
    }

    @Test
    void unconfiguredSecretFailsClosed() {
        SignupChallengeService unconfigured = new SignupChallengeService(store, sender, new SignupCodeVerifier(""));
        assertThatThrownBy(() -> unconfigured.start("user@example.com", "hash", "192.0.2.1"))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getExceptionCode()).isEqualTo(ExceptionCode.SIGNUP_MAIL_UNAVAILABLE));
        verifyNoInteractions(store, sender);
    }

    @Test
    void redisFailureBeforeMailIsServiceUnavailable() {
        when(store.reserveNew(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new DataAccessResourceFailureException("redis down"));
        assertThatThrownBy(() -> service.start("user@example.com", "hash", "192.0.2.1"))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getExceptionCode()).isEqualTo(ExceptionCode.SIGNUP_CHALLENGE_UNAVAILABLE));
        verifyNoInteractions(sender);
    }
}
