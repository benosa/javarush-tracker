package com.javarush.jira.login.internal.web;

import com.javarush.jira.AbstractControllerTest;
import com.javarush.jira.login.internal.UserRepository;
import com.javarush.jira.login.internal.jwt.TokenCookies;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends AbstractControllerTest {
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Value("${app.jwt.secret}") String secret;

    private String login(String email, String password) throws Exception {
        var result = perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new AuthController.LoginRequest(email, password))))
                .andExpect(status().isOk()).andExpect(cookie().httpOnly(TokenCookies.ACCESS, true))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax"))).andReturn();
        assertNull(result.getRequest().getSession(false));
        assertNull(result.getResponse().getCookie("JSESSIONID"));
        return json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
    private String login() throws Exception { return login("user@gmail.com", "password"); }

    @Test void authenticatesApiWithBearer() throws Exception {
        perform(get("/api/profile").header("Authorization", "Bearer " + login()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
    }
    @Test void rendersPageWithJwtCookie() throws Exception {
        perform(get("/view/tree").cookie(new Cookie(TokenCookies.ACCESS, login())))
                .andExpect(status().isOk()).andExpect(content().string(containsString("name=\"access-token\"")));
    }
    @Test void rejectsMissingAndInvalidTokens() throws Exception {
        perform(get("/api/profile")).andExpect(status().isUnauthorized());
        perform(get("/api/profile").header("Authorization", "Bearer malformed")).andExpect(status().isUnauthorized());
        String token = login();
        int dot = token.lastIndexOf('.') + 1;
        String tampered = token.substring(0, dot) + (token.charAt(dot) == 'A' ? "B" : "A") + token.substring(dot + 1);
        perform(get("/api/profile").header("Authorization", "Bearer " + tampered)).andExpect(status().isUnauthorized());
    }
    @Test void rejectsExpiredWrongIssuerAndWrongAudienceTokens() throws Exception {
        for (int scenario = 0; scenario < 4; scenario++) {
            JwtClaimsSet.Builder builder = JwtClaimsSet.builder().subject("1")
                    .issuer(scenario == 1 ? "other-service" : "jirarush")
                    .audience(List.of(scenario == 2 ? "other-service" : "jirarush"))
                    .issuedAt(Instant.now().minusSeconds(7200));
            if (scenario != 3) builder.expiresAt(Instant.now().plusSeconds(scenario == 0 ? -3600 : 3600));
            var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256")));
            String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), builder.build())).getTokenValue();
            perform(get("/api/profile").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        }
    }
    @Test void ignoresLegacySessionAuthentication() throws Exception {
        MockHttpSession session = new MockHttpSession();
        var user = new com.javarush.jira.login.AuthUser(users.getExisted(1));
        var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        session.setAttribute("SPRING_SECURITY_CONTEXT", context);
        perform(get("/api/profile").session(session)).andExpect(status().isUnauthorized());
    }
    @Test void basicAuthenticationNoLongerWorks() throws Exception {
        perform(get("/api/profile").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("user@gmail.com", "password")))
                .andExpect(status().isUnauthorized());
    }
    @Test void enforcesRolesFromCurrentDatabaseState() throws Exception {
        String user = login();
        perform(get("/api/admin/users").header("Authorization", "Bearer " + user)).andExpect(status().isForbidden());
        String adminToken = login("admin@gmail.com", "admin");
        perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
        var admin = users.getExisted(2); admin.setRoles(java.util.Set.of(com.javarush.jira.login.Role.DEV)); users.save(admin);
        perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken)).andExpect(status().isForbidden());
        perform(get("/api/admin/users").header("Authorization", "Bearer " + login("manager@gmail.com", "manager")))
                .andExpect(status().isForbidden());
    }
    @Test void rejectsDisabledUsersAndTheirTokens() throws Exception {
        String token = login();
        var user = users.getExisted(1); user.setEnabled(false); users.save(user);
        perform(get("/api/profile").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"user@gmail.com\",\"password\":\"password\"}")).andExpect(status().isUnauthorized());
    }
    @Test void passwordChangeInvalidatesTokens() throws Exception {
        String token = login();
        var user = users.getExisted(1); user.setPassword("{noop}new-password"); users.save(user);
        perform(get("/api/profile").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }
    @Test void logoutRevokesAllExistingTokens() throws Exception {
        String first = login(), second = login();
        perform(post("/api/auth/logout").header("Authorization", "Bearer " + first))
                .andExpect(status().isNoContent()).andExpect(cookie().maxAge(TokenCookies.ACCESS, 0));
        perform(get("/api/profile").header("Authorization", "Bearer " + first)).andExpect(status().isUnauthorized());
        perform(get("/api/profile").header("Authorization", "Bearer " + second)).andExpect(status().isUnauthorized());
        perform(get("/api/profile").header("Authorization", "Bearer " + login())).andExpect(status().isOk());
    }
    @Test void rejectsWrongPasswordAndUnknownAccount() throws Exception {
        for (String email : List.of("user@gmail.com", "missing@example.invalid")) {
            perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(new AuthController.LoginRequest(email, "wrong"))))
                    .andExpect(status().isUnauthorized());
        }
    }
    @Test void cookieAloneCannotMutateState() throws Exception {
        String token = login();
        perform(post("/api/auth/logout").cookie(new Cookie(TokenCookies.ACCESS, token))).andExpect(status().isForbidden());
        perform(post("/api/auth/logout").cookie(new Cookie(TokenCookies.ACCESS, token))
                .header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
    }
    @Test void rejectsCrossOriginLoginAndMutations() throws Exception {
        perform(post("/api/auth/login").header("Origin", "https://foreign.example")
                .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"user@gmail.com\",\"password\":\"password\"}"))
                .andExpect(status().isForbidden());
        perform(post("/api/auth/logout").header("Authorization", "Bearer " + login())
                .header("Sec-Fetch-Site", "cross-site")).andExpect(status().isForbidden());
    }
    @Test void oauthRedirectDoesNotCreateSession() throws Exception {
        var result = perform(get("/oauth2/authorization/github")).andExpect(status().is3xxRedirection())
                .andExpect(cookie().httpOnly("JR_OAUTH_STATE", true)).andReturn();
        assertNull(result.getRequest().getSession(false));
        assertNull(result.getResponse().getCookie("JSESSIONID"));
        assertTrue(result.getResponse().getRedirectedUrl().startsWith("https://github.com/login/oauth/authorize"));
    }
    @Test void invalidOauthCallbackDoesNotAuthenticate() throws Exception {
        var result = perform(get("/login/oauth2/code/github").param("code", "invalid").param("state", "missing"))
                .andExpect(redirectedUrl("/view/login?error")).andReturn();
        assertNull(result.getRequest().getSession(false));
        assertNull(result.getResponse().getCookie(TokenCookies.ACCESS));
    }
}
