package de.flogehring.peelserver.impl.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

public final class OutboundMailBudget {
    private final int hourlyLimit;
    private final Deque<Instant> attempts = new ArrayDeque<>();

    public OutboundMailBudget(int hourlyLimit) {
        if (hourlyLimit < 1) throw new IllegalArgumentException("The hourly mail budget must be positive");
        this.hourlyLimit = hourlyLimit;
    }

    public synchronized void reserve() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(Duration.ofHours(1));
        while (!attempts.isEmpty() && !attempts.getFirst().isAfter(cutoff)) {
            attempts.removeFirst();
        }
        if (attempts.size() >= hourlyLimit) {
            long retryAfter = Math.max(1, Duration.between(now, attempts.getFirst().plus(Duration.ofHours(1))).toSeconds() + 1);
            throw new AuthRequestException(429, "rate_limited", "Email sending is temporarily limited. Please try again later.", retryAfter);
        }
        attempts.addLast(now);
    }
}
