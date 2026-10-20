package com.tagnote.application.auth.port;

public interface SignupMailSender {
    void sendCode(String normalizedEmail, String code);
}
