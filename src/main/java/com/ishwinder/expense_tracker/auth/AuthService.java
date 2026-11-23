package com.ishwinder.expense_tracker.auth;

import com.ishwinder.expense_tracker.common.BadRequestException;
import com.ishwinder.expense_tracker.common.ConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
public class AuthService {

    private static final int BCRYPT_MAX_BYTES = 72;

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public MeResponse register(RegisterRequest request) {
        // BCrypt only uses the first 72 BYTES; accented/emoji characters take more than one
        if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new BadRequestException(
                    "Password is too long (max 72 bytes; special characters count extra)");
        }
        if (repository.existsByUsername(request.username())) {
            throw new ConflictException("Username is already taken");
        }
        try {
            AppUser saved = repository.saveAndFlush(
                    new AppUser(request.username(), passwordEncoder.encode(request.password())));
            return new MeResponse(saved.getUsername());
        } catch (DataIntegrityViolationException e) {
            // two people registering the same name at the same instant: the DB unique key wins
            throw new ConflictException("Username is already taken");
        }
    }
}