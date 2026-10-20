package com.tagnote.application.auth.port;

public interface SignupChallengeStore {
    // A reservation is unusable until the mail provider accepts the message.
    int reserveNew(String id, String email, String passwordHash, String verifier, String emailKey, String ipKey);
    int reserveResend(String id, String verifier, String emailKey, String ipKey);
    boolean activate(String id, String verifier);
    void discard(String id, String verifier);
    Verification verify(String id, String verifier);
    Claim claim(String id, String verifier, String owner);
    void release(String id, String verifier, String owner);
    void complete(String id, String verifier, String owner, Long userId);
    boolean allowAvailability(String emailKey, String ipKey);

    record Claim(Status status, String email, String passwordHash, Long userId) {
        public enum Status { CLAIMED, COMPLETE, INVALID, EXPIRED, BUSY }
    }

    record Verification(Status status, String email, String passwordHash) {
        public enum Status { VALID, INVALID, EXPIRED, PENDING }
    }
}
