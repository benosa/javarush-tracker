package com.javarush.jira.login.internal.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

// Provider tokens are used only during sign-in; JiraRush issues its own access token.
public class NoSessionAuthorizedClients implements OAuth2AuthorizedClientRepository {
    @Override public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String id, Authentication principal,
            HttpServletRequest request) { return null; }
    @Override public void saveAuthorizedClient(OAuth2AuthorizedClient client, Authentication principal,
            HttpServletRequest request, HttpServletResponse response) { }
    @Override public void removeAuthorizedClient(String id, Authentication principal,
            HttpServletRequest request, HttpServletResponse response) { }
}
