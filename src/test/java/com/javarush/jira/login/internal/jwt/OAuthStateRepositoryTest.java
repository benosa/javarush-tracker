package com.javarush.jira.login.internal.jwt;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import static org.junit.jupiter.api.Assertions.*;

class OAuthStateRepositoryTest {
    @Test void bindsStateToBrowserAndConsumesItOnce() {
        var repository = new OAuthStateRepository(new TokenCookies(false));
        var authorization = OAuth2AuthorizationRequest.authorizationCode().authorizationUri("https://provider.example/auth")
                .clientId("client").redirectUri("http://localhost/login/oauth2/code/provider").state("random-state").build();
        var start = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        repository.saveAuthorizationRequest(authorization, start, response);
        assertNull(start.getSession(false));
        var callback = new MockHttpServletRequest();
        callback.setParameter("state", "random-state");
        assertNull(repository.loadAuthorizationRequest(callback));
        callback.setCookies(new Cookie("JR_OAUTH_STATE", "wrong-browser"));
        assertNull(repository.loadAuthorizationRequest(callback));
        callback.setCookies(new Cookie("JR_OAUTH_STATE", "random-state"));
        assertSame(authorization, repository.removeAuthorizationRequest(callback, response));
        assertNull(repository.removeAuthorizationRequest(callback, response));
        assertNull(callback.getSession(false));
    }
}
