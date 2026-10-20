package com.tagnote.application.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class SignupCodeVerifier {
    private final byte[] secret;

    public SignupCodeVerifier(@Value("${signup.code-secret:}") String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            // Deliberately fail closed when signup is called; other auth flows can still start.
            this.secret = null;
        } else {
            this.secret = secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    public boolean configured() {
        return secret != null;
    }

    public String sign(String purpose, String value) {
        if (!configured()) throw new IllegalStateException("Signup code secret is not configured");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((purpose + ":" + value).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot calculate signup verifier", e);
        }
    }

    public boolean matches(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), actual.getBytes(StandardCharsets.US_ASCII));
    }
}
