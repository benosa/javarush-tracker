package com.javarush.jira;

import com.javarush.jira.login.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import java.util.Locale;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LocalizationTest extends AbstractControllerTest {
    @Autowired SpringTemplateEngine templates;

    @Test void changesHomeLanguageWithoutSession() throws Exception {
        perform(get("/").param("lang", "en")).andExpect(status().isOk())
                .andExpect(content().string(containsString("JiraRush Home page")))
                .andExpect(cookie().value("language", "en"));
        perform(get("/").param("lang", "ru")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Главная страница JiraRush")));
    }

    @Test void localizesBothMailTemplatesAndEscapesUserInput() {
        User user = new User(1L, "test@example.invalid", "unused", "<script>bad</script>", "Test", "test");
        Map<String, Object> params = Map.of("user", user, "confirmationUrl", "http://localhost/confirm?token=abc", "resetUrl", "http://localhost/reset?token=def");
        for (String name : new String[]{"email-confirmation", "password-reset"}) {
            String en = templates.process(name, new Context(Locale.ENGLISH, params));
            String ru = templates.process(name, new Context(Locale.forLanguageTag("ru"), params));
            assertTrue(en.contains("Hello,"));
            assertTrue(ru.contains("Привет,"));
            assertFalse(en.contains("<script>bad</script>"));
            assertTrue(en.contains("&lt;script&gt;"));
            assertTrue(en.contains(name.equals("email-confirmation") ? "Confirm email" : "Set a new password"));
            assertTrue(ru.contains(name.equals("email-confirmation") ? "Подтвердить почту" : "Установить пароль"));
        }
    }
}
