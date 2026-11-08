package com.javarush.jira;

import com.javarush.jira.ref.RefType;
import com.javarush.jira.ref.ReferenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SocialProviderRemovalTest extends AbstractControllerTest {
    @Autowired ClientRegistrationRepository registrations;
    @Autowired JdbcTemplate jdbc;

    @Test void loginAndRegistrationOnlyOfferSupportedProviders() throws Exception {
        for (String page : new String[]{"/view/login", "/ui/register"}) {
            perform(get(page)).andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("/oauth2/authorization/vk"))))
                    .andExpect(content().string(not(containsString("/oauth2/authorization/yandex"))))
                    .andExpect(content().string(containsString("/oauth2/authorization/google")))
                    .andExpect(content().string(containsString("/oauth2/authorization/github")))
                    .andExpect(content().string(containsString("/oauth2/authorization/gitlab")));
        }
        assertNull(registrations.findByRegistrationId("vk"));
        assertNull(registrations.findByRegistrationId("yandex"));
    }

    @Test void retiredSocialContactsAreAbsentFromDatabaseAndReferenceCache() {
        assertFalse(ReferenceService.getRefs(RefType.CONTACT).containsKey("vk"));
        assertFalse(ReferenceService.getRefs(RefType.CONTACT).containsKey("yandex"));
        assertEquals(0, jdbc.queryForObject("select count(*) from CONTACT where CODE in ('vk', 'yandex')", Integer.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from REFERENCE where REF_TYPE = 0 and CODE in ('vk', 'yandex')", Integer.class));
    }
}
