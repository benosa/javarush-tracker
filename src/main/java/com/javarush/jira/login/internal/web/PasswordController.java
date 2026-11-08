package com.javarush.jira.login.internal.web;

import com.javarush.jira.login.User;
import com.javarush.jira.login.internal.OneTimeActionService;
import com.javarush.jira.login.internal.UserRepository;
import com.javarush.jira.login.internal.passwordreset.PasswordResetEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Controller
@Validated
@RequestMapping("/ui/password")
@RequiredArgsConstructor
public class PasswordController {
    private final UserRepository users;
    private final OneTimeActionService actions;
    private final ApplicationEventPublisher events;

    @PostMapping("/reset")
    public String resetPassword(@RequestParam String email) {
        users.findByEmailIgnoreCase(email).filter(User::isEnabled).ifPresent(user ->
                events.publishEvent(new PasswordResetEvent(user, actions.requestPasswordReset(user))));
        return "redirect:/view/login";
    }

    @GetMapping("/change")
    public String changePassword(@RequestParam String token, Model model) {
        actions.checkReset(token);
        model.addAttribute("token", token);
        return "unauth/change-password";
    }

    @PostMapping("/save")
    public String savePassword(@RequestParam String token,
                              @RequestParam @NotBlank @Size(min = 5, max = 32) String password) {
        actions.resetPassword(token, password);
        return "redirect:/view/login?reset";
    }
}
