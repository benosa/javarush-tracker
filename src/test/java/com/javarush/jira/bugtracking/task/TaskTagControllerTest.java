package com.javarush.jira.bugtracking.task;

import com.javarush.jira.AbstractControllerTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithUserDetails;
import java.util.Map;
import static com.javarush.jira.common.util.JsonUtil.writeValue;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithUserDetails("user@gmail.com")
class TaskTagControllerTest extends AbstractControllerTest {
    @Autowired TaskTagService service;
    @Autowired TaskRepository repository;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;

    @Test void addsTagToTaskCreatedInTheSameTransaction() {
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(status -> {
            Task task = repository.saveAndFlush(new Task(null, "New task", "task", "todo", null, 1, 1L));
            assertEquals(java.util.Set.of("backend"), service.addTag(task.id(), "backend"));
        });
    }

    private static final String URL = "/api/tasks/1/tags";

    @Test void addsAndReadsTagsWithoutDuplicates() throws Exception {
        for (String tag : new String[]{"backend", "backend", " api "}) {
            perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(writeValue(Map.of("tag", tag))))
                    .andExpect(status().isOk());
        }
        perform(get(URL)).andExpect(status().isOk()).andExpect(jsonPath("$", contains("api", "backend")));
        assertEquals(2, service.getTags(1).size());
        assertTrue(service.getTags(2).isEmpty());
    }

    @Test void deletesOnlyRequestedTagAndIsIdempotent() throws Exception {
        service.addTag(1, "backend"); service.addTag(1, "api");
        perform(delete(URL + "/backend")).andExpect(status().isNoContent());
        perform(delete(URL + "/backend")).andExpect(status().isNoContent());
        perform(get(URL)).andExpect(jsonPath("$", contains("api")));
    }

    @Test void reportsMissingTask() throws Exception {
        perform(post("/api/tasks/9999/tags").contentType(MediaType.APPLICATION_JSON).content("{\"tag\":\"backend\"}"))
                .andExpect(status().isNotFound());
        perform(get("/api/tasks/9999/tags")).andExpect(status().isNotFound());
    }

    @ParameterizedTest @ValueSource(strings = {"", " ", "x", "<b>tag</b>", "abcdefghijklmnopqrstuvwxyz0123456789"})
    void rejectsInvalidTag(String tag) throws Exception {
        perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(writeValue(Map.of("tag", tag))))
                .andExpect(status().isUnprocessableEntity());
        assertTrue(service.getTags(1).isEmpty());
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"x", "<b>tag</b>"})
    void serviceAlsoValidatesInput(String tag) {
        assertThrows(com.javarush.jira.common.error.IllegalRequestDataException.class, () -> service.addTag(1, tag));
    }

    @Test @WithAnonymousUser void rejectsAnonymousRequests() throws Exception {
        perform(get(URL)).andExpect(status().isUnauthorized());
        perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"tag\":\"api\"}"))
                .andExpect(status().isUnauthorized());
        perform(delete(URL + "/api")).andExpect(status().isUnauthorized());
    }
}
