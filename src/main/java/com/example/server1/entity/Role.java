package com.example.server1.entity;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public enum Role {
    USER,
    ADMIN;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
