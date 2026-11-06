package com.javarush.jira.login.internal.jwt;

import com.javarush.jira.login.AuthUser;
import com.javarush.jira.login.User;
import com.javarush.jira.login.internal.UserRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Service;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {
    private static final String ISSUER = "jirarush";
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final UserRepository users;
    private final Duration ttl;

    public JwtService(UserRepository users, @Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.ttl:PT30M}") Duration ttl) {
        byte[] bytes = Base64.getDecoder().decode(secret);
        if (bytes.length < 32) throw new IllegalArgumentException("JWT_SECRET must contain at least 32 random bytes, base64 encoded");
        if (ttl.compareTo(Duration.ofMinutes(1)) < 0 || ttl.compareTo(Duration.ofDays(1)) > 0)
            throw new IllegalArgumentException("JWT_TTL must be between one minute and one day");
        var key = new SecretKeySpec(bytes, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        var jwtDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        jwtDecoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(ISSUER),
                new JwtClaimValidator<List<String>>("aud", aud -> aud != null && aud.contains(ISSUER))));
        this.decoder = jwtDecoder;
        this.users = users;
        this.ttl = ttl;
    }

    public String issue(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(ISSUER).subject(user.getId().toString())
                .audience(List.of(ISSUER)).issuedAt(now).expiresAt(now.plus(ttl)).id(UUID.randomUUID().toString())
                .claim("ver", user.getTokenVersion()).claim("pwd", fingerprint(user.getPassword())).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    public JwtDecoder decoder() { return decoder; }
    public Duration ttl() { return ttl; }

    public AccessAuthentication authenticate(Jwt jwt) {
        try {
            User user = users.findById(Long.parseLong(jwt.getSubject())).orElseThrow();
            Number version = jwt.getClaim("ver");
            if (!user.isEnabled() || version == null || user.getTokenVersion() != version.longValue()
                    || !fingerprint(user.getPassword()).equals(jwt.getClaimAsString("pwd"))) {
                throw new IllegalArgumentException("Credentials changed");
            }
            return new AccessAuthentication(new AuthUser(user), jwt.getTokenValue());
        } catch (RuntimeException ex) {
            throw new InvalidBearerTokenException("Token is no longer valid");
        }
    }

    private String fingerprint(String password) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256")
                    .digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
