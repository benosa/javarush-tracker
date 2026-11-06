package com.javarush.jira.login.internal.jwt;

import com.javarush.jira.login.AuthUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class AccessAuthentication extends UsernamePasswordAuthenticationToken {
    public AccessAuthentication(AuthUser user, String token) {
        super(user, token, user.getAuthorities());
    }
    public String token() { return (String) getCredentials(); }
}
