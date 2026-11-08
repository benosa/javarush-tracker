package com.javarush.jira.login.internal.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.net.URI;
import java.util.Set;

public class BrowserRequestFilter extends OncePerRequestFilter {
    private final TokenCookies cookies;
    public BrowserRequestFilter(TokenCookies cookies) { this.cookies = cookies; }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())) {
            String origin = request.getHeader("Origin");
            boolean foreign = "cross-site".equals(request.getHeader("Sec-Fetch-Site"));
            if (origin != null) {
                try {
                    URI supplied = URI.create(origin);
                    URI current = URI.create(request.getRequestURL().toString());
                    foreign |= !supplied.getScheme().equals(current.getScheme())
                            || !supplied.getAuthority().equals(current.getAuthority());
                } catch (RuntimeException ex) { foreign = true; }
            }
            String authorization = request.getHeader("Authorization");
            boolean bearer = authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7);
            // A cookie may navigate to a page, but cannot authorize a mutation on its own.
            if (foreign || (cookies.read(request, TokenCookies.ACCESS) != null
                    && !bearer
                    && !request.getRequestURI().equals("/api/auth/login"))) {
                response.sendError(403);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
