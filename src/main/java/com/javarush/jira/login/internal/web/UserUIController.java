package com.javarush.jira.login.internal.web;

import com.javarush.jira.common.error.IllegalRequestDataException;
import com.javarush.jira.login.AuthUser;
import com.javarush.jira.login.UserTo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping(UserUIController.USER_URL)
@RequiredArgsConstructor
public class UserUIController extends AbstractUserController {
    public static final String USER_URL = "/ui/user";
    private static final String REDIRECT_PROFILE = "/ui/profile";
    private final com.javarush.jira.login.internal.jwt.TokenCookies cookies;

    @PostMapping
    public String update(@Valid UserTo user, BindingResult result, @AuthenticationPrincipal AuthUser authUser,
                         RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) {
            redirectAttrs.addAttribute("userResult", "invalid");
            return "redirect:" + REDIRECT_PROFILE;
        }
        authUser.setUser(handler.updateFromTo(user, authUser.id()));
        redirectAttrs.addAttribute("userResult", "saved");
        return "redirect:" + REDIRECT_PROFILE;
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid PasswordChange passwordChange, BindingResult result,
                                 @AuthenticationPrincipal AuthUser authUser, RedirectAttributes redirectAttrs,
                                 jakarta.servlet.http.HttpServletResponse response) {
        if (result.hasErrors()) {
            redirectAttrs.addAttribute("userResult", "invalid");
            return "redirect:" + REDIRECT_PROFILE;
        }
        try {
            changePassword0(passwordChange.oldPassword, passwordChange.newPassword, authUser.id());
        } catch (IllegalRequestDataException e) {
            redirectAttrs.addAttribute("userResult", "wrong-password");
            return "redirect:" + REDIRECT_PROFILE;
        }
        cookies.clear(response, com.javarush.jira.login.internal.jwt.TokenCookies.ACCESS);
        return "redirect:/view/login?passwordChanged";
    }

    public record PasswordChange(String oldPassword, @Size(min = 5, max = 128) String newPassword) {
    }
}
