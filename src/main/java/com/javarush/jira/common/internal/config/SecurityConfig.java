package com.javarush.jira.common.internal.config;

import com.javarush.jira.login.AuthUser;
import com.javarush.jira.login.internal.UserRepository;
import com.javarush.jira.login.internal.jwt.*;
import com.javarush.jira.login.internal.sociallogin.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    public static final PasswordEncoder PASSWORD_ENCODER = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private final UserRepository users;
    private final JwtService jwt;
    private final TokenCookies cookies;
    private final OAuthStateRepository oauthStates;
    private final CustomOAuth2UserService oauthUsers;

    @Bean public PasswordEncoder passwordEncoder() { return PASSWORD_ENCODER; }

    @Bean public UserDetailsService userDetailsService() {
        return email -> new AuthUser(users.findByEmailIgnoreCase(email.toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Bad credentials")));
    }

    @Bean public AuthenticationManager authenticationManager(UserDetailsService details, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(details);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint entryPoint = (request, response, exception) -> {
            if (request.getRequestURI().startsWith("/api/")) {
                response.setStatus(401);
            } else {
                cookies.clear(response, TokenCookies.ACCESS);
                response.sendRedirect("/view/login?expired");
            }
        };
        DefaultBearerTokenResolver headers = new DefaultBearerTokenResolver();
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/view/login", "/api/auth/login", "/oauth2/**", "/login/oauth2/**",
                        "/doc", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/static/**", "/robots.txt").permitAll()
                .requestMatchers("/view/unauth/**", "/ui/register/**", "/ui/password/**").anonymous()
                .requestMatchers("/api/admin/**", "/ui/admin/**", "/view/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/mngr/**", "/ui/mngr/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.POST, "/api/users").anonymous()
                .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .csrf(csrf -> csrf.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint))
                .addFilterBefore(new BrowserRequestFilter(cookies), BearerTokenAuthenticationFilter.class)
                .oauth2ResourceServer(resource -> resource
                        .authenticationEntryPoint(entryPoint)
                        .bearerTokenResolver(request -> {
                            String token = headers.resolve(request);
                            if (token != null) return token;
                            String path = request.getRequestURI();
                            if (path.equals("/view/login") || path.equals("/api/auth/login")
                                    || path.startsWith("/static/") || path.startsWith("/oauth2/")
                                    || path.startsWith("/login/oauth2/") || path.startsWith("/swagger-ui/")
                                    || path.startsWith("/v3/api-docs") || path.equals("/doc")) return null;
                            return cookies.read(request, TokenCookies.ACCESS);
                        })
                        .jwt(tokens -> tokens.decoder(jwt.decoder()).jwtAuthenticationConverter(jwt::authenticate)))
                .oauth2Login(oauth -> oauth.loginPage("/view/login")
                        .authorizationEndpoint(endpoint -> endpoint.authorizationRequestRepository(oauthStates))
                        .authorizedClientRepository(new NoSessionAuthorizedClients())
                        .userInfoEndpoint(info -> info.userService(oauthUsers))
                        .successHandler((request, response, auth) -> {
                            AuthUser user = (AuthUser) auth.getPrincipal();
                            if (!user.isEnabled()) { response.sendError(401); return; }
                            cookies.write(response, TokenCookies.ACCESS, jwt.issue(user.getUser()), jwt.ttl());
                            response.sendRedirect("/");
                        })
                        .failureHandler((request, response, exception) -> response.sendRedirect("/view/login?error")));
        return http.build();
    }
}
