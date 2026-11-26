package com.ishwinder.expense_tracker.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-24T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private MutableClock clock;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        service = new LoginAttemptService(clock);
    }

    private void fail(String key, int times) {
        for (int i = 0; i < times; i++) {
            service.recordFailure(key);
        }
    }

    @Test
    void isNotBlocked_afterFourFailures_butBlocked_afterFive() {
        String key = LoginAttemptService.key("10.0.0.1", "alice");

        fail(key, 4);
        assertThat(service.isBlocked(key)).isFalse();

        fail(key, 1);
        assertThat(service.isBlocked(key)).isTrue();
    }

    @Test
    void keysAreIndependent_andUsernamesAreCaseInsensitive() {
        fail(LoginAttemptService.key("10.0.0.1", "Alice"), 5);

        assertThat(service.isBlocked(LoginAttemptService.key("10.0.0.1", "alice"))).isTrue();
        assertThat(service.isBlocked(LoginAttemptService.key("10.0.0.1", "bob"))).isFalse();
        assertThat(service.isBlocked(LoginAttemptService.key("10.0.0.2", "alice"))).isFalse();
    }

    @Test
    void unblocksAfterTheWindowPasses() {
        String key = LoginAttemptService.key("10.0.0.1", "alice");
        fail(key, 5);

        clock.advance(Duration.ofMinutes(14));
        assertThat(service.isBlocked(key)).isTrue();

        clock.advance(Duration.ofMinutes(1));
        assertThat(service.isBlocked(key)).isFalse();
    }

    @Test
    void successClearsTheCounter() {
        String key = LoginAttemptService.key("10.0.0.1", "alice");
        fail(key, 4);

        service.recordSuccess(key);
        fail(key, 4);

        assertThat(service.isBlocked(key)).isFalse();
    }

    @Test
    void retryAfterSeconds_countsDownTowardsTheEndOfTheWindow() {
        String key = LoginAttemptService.key("10.0.0.1", "alice");
        fail(key, 5);
        assertThat(service.retryAfterSeconds(key)).isEqualTo(15 * 60);

        clock.advance(Duration.ofMinutes(10));

        assertThat(service.retryAfterSeconds(key)).isEqualTo(5 * 60);
    }
}