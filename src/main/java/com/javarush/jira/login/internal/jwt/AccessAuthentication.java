package com.javarush.jira.login.internal.jwt;

import com.javarush.jira.login.AuthUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class AccessAuthentication extends UsernamePasswordAuthenticationToken {
    private final String token;
    public AccessAuthentication(AuthUser user, String token) {
        super(user, null, user.getAuthorities());
        this.token = token;
    }
    public String token() { return token; }
}
