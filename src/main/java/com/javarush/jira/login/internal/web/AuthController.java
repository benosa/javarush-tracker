package com.javarush.jira.login.internal.web;

import com.javarush.jira.login.AuthUser;
import com.javarush.jira.login.internal.UserRepository;
import com.javarush.jira.login.internal.jwt.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authentication;
    private final JwtService jwt;
    private final TokenCookies cookies;
    private final UserRepository users;

    @PostMapping(value = "/login", consumes = "application/json")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        var result = authentication.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        String token = jwt.issue(((AuthUser) result.getPrincipal()).getUser());
        cookies.write(response, TokenCookies.ACCESS, token, jwt.ttl());
        response.setHeader("Cache-Control", "no-store");
        return new TokenResponse(token, "Bearer", jwt.ttl().toSeconds());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal AuthUser user, HttpServletResponse response) {
        users.invalidateTokens(user.id());
        cookies.clear(response, TokenCookies.ACCESS);
    }

    public record LoginRequest(@NotBlank @Size(max = 128) String username,
                               @NotBlank @Size(max = 128) String password) { }
    public record TokenResponse(String accessToken, String tokenType, long expiresIn) { }
}
