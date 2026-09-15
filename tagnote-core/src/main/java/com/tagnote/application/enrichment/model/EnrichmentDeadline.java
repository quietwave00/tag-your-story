package com.tagnote.application.enrichment.model;

import java.util.concurrent.TimeUnit;

public final class EnrichmentDeadline {

    private final long deadlineNanos;

    private EnrichmentDeadline(long deadlineNanos) {
        this.deadlineNanos = deadlineNanos;
    }

    public static EnrichmentDeadline afterMillis(long budgetMs) {
        if (budgetMs <= 0) {
            throw new IllegalArgumentException("Enrichment deadline budget must be positive");
        }
        long now = System.nanoTime();
        long budgetNanos = TimeUnit.MILLISECONDS.toNanos(budgetMs);
        try {
            return new EnrichmentDeadline(Math.addExact(now, budgetNanos));
        } catch (ArithmeticException overflow) {
            return new EnrichmentDeadline(Long.MAX_VALUE);
        }
    }

    public static EnrichmentDeadline expired() {
        return new EnrichmentDeadline(System.nanoTime());
    }

    public long remainingMillis() {
        long remainingNanos = remainingNanos();
        if (remainingNanos == 0) {
            return 0;
        }
        long millis = TimeUnit.NANOSECONDS.toMillis(remainingNanos);
        return millis == 0 ? 1 : millis;
    }

    public boolean hasTimeFor(long requiredMs) {
        if (requiredMs < 0) {
            throw new IllegalArgumentException("Required enrichment time must not be negative");
        }
        return remainingNanos() > TimeUnit.MILLISECONDS.toNanos(requiredMs);
    }

    private long remainingNanos() {
        long remaining = deadlineNanos - System.nanoTime();
        return remaining > 0 ? remaining : 0;
    }
}
