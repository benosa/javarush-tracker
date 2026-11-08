package com.javarush.jira.login.internal;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.javarush.jira.common.error.IllegalRequestDataException;
import com.javarush.jira.login.User;
import com.javarush.jira.login.UserTo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OneTimeActionService {
    private final UserRepository users;
    private final UserMapper mapper;
    private final PasswordEncoder encoder;
    private final Cache<String, User> registrations = Caffeine.newBuilder()
            .maximumSize(1000).expireAfterWrite(Duration.ofMinutes(15)).build();
    private final Cache<String, ResetTarget> resets = Caffeine.newBuilder()
            .maximumSize(1000).expireAfterWrite(Duration.ofMinutes(15)).build();

    public String register(UserTo input) {
        User user = mapper.toEntity(input);
        user.normalize();
        user.setPassword(encoder.encode(input.getPassword()));
        String token = UUID.randomUUID().toString();
        registrations.put(token, user);
        return token;
    }

    @Transactional
    public void confirm(String token) {
        User user = registrations.asMap().remove(token);
        if (user == null) throw invalid();
        users.save(user);
    }

    public String requestPasswordReset(User user) {
        String token = UUID.randomUUID().toString();
        resets.put(token, new ResetTarget(user.id(), user.getPassword()));
        return token;
    }

    public void checkReset(String token) {
        ResetTarget target = resets.getIfPresent(token);
        if (target == null || !valid(target)) throw invalid();
    }

    @Transactional
    public void resetPassword(String token, String password) {
        ResetTarget target = resets.asMap().remove(token);
        if (target == null || !valid(target)) throw invalid();
        User user = users.getExisted(target.id());
        user.setPassword(encoder.encode(password));
        users.save(user);
        users.invalidateTokens(user.id());
    }

    private boolean valid(ResetTarget target) {
        return users.findById(target.id()).filter(User::isEnabled)
                .filter(user -> user.getPassword().equals(target.password())).isPresent();
    }
    private IllegalRequestDataException invalid() { return new IllegalRequestDataException("Link expired or already used"); }
    private record ResetTarget(long id, String password) { }
}
