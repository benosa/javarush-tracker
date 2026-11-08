package com.javarush.jira.login.internal.web;

import com.javarush.jira.AbstractControllerTest;
import com.javarush.jira.login.internal.OneTimeActionService;
import com.javarush.jira.login.internal.UserRepository;
import com.javarush.jira.login.internal.passwordreset.PasswordResetEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RecordApplicationEvents
class PasswordControllerTest extends AbstractControllerTest {
    @Autowired ApplicationEvents events;
    @Autowired UserRepository users;
    @Autowired OneTimeActionService actions;
    @Autowired PasswordEncoder encoder;

    @Test void resetsFromAnotherBrowserWithoutSessionAndRejectsReuse() throws Exception {
        var result = perform(post("/ui/password/reset").param("email", "user@gmail.com"))
                .andExpect(redirectedUrl("/view/login")).andReturn();
        assertNull(result.getRequest().getSession(false));
        String token = events.stream(PasswordResetEvent.class).findFirst().orElseThrow().token();
        perform(get("/ui/password/change").param("token", token)).andExpect(status().isOk());
        perform(post("/ui/password/save").param("token", token).param("password", "changed-password"))
                .andExpect(redirectedUrl("/view/login?reset"));
        assertTrue(encoder.matches("changed-password", users.getExisted(1).getPassword()));
        perform(post("/ui/password/save").param("token", token).param("password", "another-password"))
                .andExpect(status().isUnprocessableEntity());
    }
    @Test void doesNotRevealUnknownEmail() throws Exception {
        perform(post("/ui/password/reset").param("email", "missing@example.invalid"))
                .andExpect(redirectedUrl("/view/login"));
        assertEquals(0, events.stream(PasswordResetEvent.class).count());
    }
    @Test void rejectsInvalidLink() throws Exception {
        perform(get("/ui/password/change").param("token", "invalid")).andExpect(status().isUnprocessableEntity());
    }
    @Test void invalidPasswordDoesNotConsumeLink() throws Exception {
        String token = actions.requestPasswordReset(users.getExisted(1));
        perform(post("/ui/password/save").param("token", token).param("password", "x"))
                .andExpect(status().isUnprocessableEntity());
        perform(post("/ui/password/save").param("token", token).param("password", "valid-password"))
                .andExpect(status().isFound());
    }
    @Test void oldResetLinksStopWorkingAfterPasswordChanges() {
        String first = actions.requestPasswordReset(users.getExisted(1));
        String second = actions.requestPasswordReset(users.getExisted(1));
        actions.resetPassword(first, "changed-password");
        assertThrows(com.javarush.jira.common.error.IllegalRequestDataException.class, () -> actions.checkReset(second));
    }
}
