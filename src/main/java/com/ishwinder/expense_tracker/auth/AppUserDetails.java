package com.ishwinder.expense_tracker.auth;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

public class AppUserDetails extends User {

    private final Long id;

    public AppUserDetails(Long id, String username, String passwordHash) {
        super(username, passwordHash, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        this.id = id;
    }

    public Long getId() { return id; }
}