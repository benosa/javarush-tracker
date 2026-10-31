package com.javarush.jira.profile.internal.web;

import com.javarush.jira.AbstractControllerTest;
import com.javarush.jira.profile.ProfileTo;
import com.javarush.jira.profile.ContactTo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.security.test.context.support.WithAnonymousUser;
import java.util.Set;
import java.util.stream.Stream;
import static com.javarush.jira.common.util.JsonUtil.writeValue;
import static com.javarush.jira.login.internal.web.UserTestData.*;
import static com.javarush.jira.profile.internal.web.ProfileTestData.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithUserDetails(USER_MAIL)
class ProfileRestControllerTest extends AbstractControllerTest {
    private static final String URL = "/api/profile";

    @Test void readsOwnProfile() throws Exception {
        perform(get(URL)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.mailNotifications", containsInAnyOrder("assigned", "overdue", "deadline")))
                .andExpect(jsonPath("$.contacts", hasSize(3)));
    }

    @Test @WithUserDetails(GUEST_MAIL)
    void returnsEmptyProfileWhenNotCreatedYet() throws Exception {
        perform(get(URL)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(GUEST_ID))
                .andExpect(jsonPath("$.mailNotifications").isEmpty())
                .andExpect(jsonPath("$.contacts").isEmpty());
    }

    @Test void updatesProfile() throws Exception {
        perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(writeValue(getUpdatedTo())))
                .andExpect(status().isNoContent());
        perform(get(URL)).andExpect(jsonPath("$.mailNotifications", hasSize(6)))
                .andExpect(jsonPath("$.contacts", hasSize(7)))
                .andExpect(jsonPath("$.contacts[?(@.code == 'skype')].value", contains("newSkype")));
    }

    @Test @WithUserDetails(GUEST_MAIL)
    void createsProfileOnFirstUpdate() throws Exception {
        perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(writeValue(getNewTo())))
                .andExpect(status().isNoContent());
        perform(get(URL)).andExpect(jsonPath("$.id").value(GUEST_ID))
                .andExpect(jsonPath("$.contacts[0].value").value("guestTg"));
    }

    @Test void clearsContactsAndNotifications() throws Exception {
        perform(put(URL).contentType(MediaType.APPLICATION_JSON)
                .content(writeValue(new ProfileTo(null, Set.of(), Set.of())))).andExpect(status().isNoContent());
        perform(get(URL)).andExpect(jsonPath("$.contacts").isEmpty()).andExpect(jsonPath("$.mailNotifications").isEmpty());
    }

}
