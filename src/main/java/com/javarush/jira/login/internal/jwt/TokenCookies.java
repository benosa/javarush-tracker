package com.javarush.jira.login.internal.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
public class TokenCookies {
    public static final String ACCESS = "JR_ACCESS";
    private final boolean secure;
    public TokenCookies(@Value("${app.jwt.secure-cookie:false}") boolean secure) { this.secure = secure; }

    public String read(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
    public void write(HttpServletResponse response, String name, String value, Duration ttl) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(name, value).path("/")
                .httpOnly(true).secure(secure).sameSite("Lax").maxAge(ttl).build().toString());
    }
    public void clear(HttpServletResponse response, String name) { write(response, name, "", Duration.ZERO); }
}
