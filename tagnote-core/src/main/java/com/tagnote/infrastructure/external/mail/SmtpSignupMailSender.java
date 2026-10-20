package com.tagnote.infrastructure.external.mail;

import com.tagnote.application.auth.port.SignupMailSender;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class SmtpSignupMailSender implements SignupMailSender {
    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${signup.mail.from:}")
    private String from;

    @Value("${spring.mail.host:}")
    private String host;

    @Override
    public void sendCode(String normalizedEmail, String code) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!StringUtils.hasText(from) || !StringUtils.hasText(host) || sender == null) {
            throw new CustomException(ExceptionCode.SIGNUP_MAIL_UNAVAILABLE);
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(normalizedEmail);
        message.setSubject("TagNote 가입 인증 코드");
        message.setText("가입 인증 코드는 " + code + "입니다. 10분 이내에 입력해 주세요.");
        sender.send(message);
    }
}
