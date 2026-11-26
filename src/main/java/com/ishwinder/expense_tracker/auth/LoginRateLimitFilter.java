package com.ishwinder.expense_tracker.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Rejects login attempts from a client that has failed too many times recently. */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    static final String LOGIN_PATH = "/api/v1/auth/login";

    private final LoginAttemptService attempts;

    public LoginRateLimitFilter(LoginAttemptService attempts) {
        this.attempts = attempts;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                && LOGIN_PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = LoginAttemptService.key(request.getRemoteAddr(), request.getParameter("username"));
        if (attempts.isBlocked(key)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(attempts.retryAfterSeconds(key)));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"status":429,"error":"TOO_MANY_ATTEMPTS","message":"Too many failed logins. Try again later."}""");
            return;
        }
        chain.doFilter(request, response);
    }
}