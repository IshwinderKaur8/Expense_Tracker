package com.ishwinder.expense_tracker.auth;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Counts failed logins per (client address + username) to slow down password guessing. */
@Service
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_TRACKED_KEYS = 10_000;

    private record Attempt(int failures, Instant firstFailureAt) { }

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public static String key(String clientAddress, String username) {
        String name = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        return clientAddress + "|" + name;
    }

    public boolean isBlocked(String key) {
        Attempt attempt = attempts.get(key);
        return attempt != null && !expired(attempt) && attempt.failures() >= MAX_FAILURES;
    }

    public void recordFailure(String key) {
        Instant now = clock.instant();
        if (attempts.size() >= MAX_TRACKED_KEYS) {
            attempts.values().removeIf(this::expired);          // keeps memory bounded
        }
        attempts.merge(key, new Attempt(1, now), (old, fresh) ->
                expired(old) ? fresh : new Attempt(old.failures() + 1, old.firstFailureAt()));
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    public long retryAfterSeconds(String key) {
        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            return 0;
        }
        Duration remaining = WINDOW.minus(Duration.between(attempt.firstFailureAt(), clock.instant()));
        return Math.max(remaining.getSeconds(), 1);
    }

    /** Used by tests. */
    public void clear() {
        attempts.clear();
    }

    private boolean expired(Attempt attempt) {
        return !clock.instant().isBefore(attempt.firstFailureAt().plus(WINDOW));
    }
}