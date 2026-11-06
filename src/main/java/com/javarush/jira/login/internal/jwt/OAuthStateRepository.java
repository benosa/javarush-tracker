package com.javarush.jira.login.internal.jwt;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.Objects;

@Component
public class OAuthStateRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    private static final String COOKIE = "JR_OAUTH_STATE";
    private final Cache<String, OAuth2AuthorizationRequest> requests = Caffeine.newBuilder()
            .maximumSize(1000).expireAfterWrite(Duration.ofMinutes(10)).build();
    private final TokenCookies cookies;
    public OAuthStateRepository(TokenCookies cookies) { this.cookies = cookies; }

    @Override public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String state = request.getParameter("state");
        if (state == null || !Objects.equals(state, cookies.read(request, COOKIE))) return null;
        return requests.getIfPresent(state);
    }
    @Override public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorization,
            HttpServletRequest request, HttpServletResponse response) {
        if (authorization == null) { removeAuthorizationRequest(request, response); return; }
        requests.put(authorization.getState(), authorization);
        cookies.write(response, COOKIE, authorization.getState(), Duration.ofMinutes(10));
    }
    @Override public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
            HttpServletResponse response) {
        OAuth2AuthorizationRequest authorization = loadAuthorizationRequest(request);
        cookies.clear(response, COOKIE);
        return authorization == null ? null : requests.asMap().remove(authorization.getState());
    }
}
