package com.tagnote.application.auth.port;

public interface LoginAttemptStore {
    boolean allow(String emailKey, String ipKey);
    void recordFailure(String emailKey);
    void clearFailures(String emailKey);
}
