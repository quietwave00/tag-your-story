package com.tagnote.infrastructure.external.mail;

import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SmtpSignupMailSenderTest {
    @Test
    void missingConfigurationReturnsExplicitServiceError() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        SmtpSignupMailSender sender = new SmtpSignupMailSender(provider);
        assertThatThrownBy(() -> sender.sendCode("user@example.com", "123456"))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getExceptionCode()).isEqualTo(ExceptionCode.SIGNUP_MAIL_UNAVAILABLE));
    }

    @Test
    void sendsToNormalizedAddressUsingConfiguredFrom() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        JavaMailSender mail = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(mail);
        SmtpSignupMailSender sender = new SmtpSignupMailSender(provider);
        ReflectionTestUtils.setField(sender, "from", "noreply@example.com");
        ReflectionTestUtils.setField(sender, "host", "smtp.example.com");

        sender.sendCode("user@example.com", "123456");

        var message = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(message.capture());
        assertThat(message.getValue().getFrom()).isEqualTo("noreply@example.com");
        assertThat(message.getValue().getTo()).containsExactly("user@example.com");
        assertThat(message.getValue().getText()).contains("123456");
    }
}
