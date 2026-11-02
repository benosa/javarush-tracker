package com.javarush.jira.bugtracking.task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskTimingServiceTest {
    @Mock ActivityRepository activities;
    @InjectMocks TaskTimingService service;
    private final Task task = new Task(10L, "Task", "task", "done", null, 1, null);

    private Activity at(int hour, String status) {
        Activity activity = new Activity(null, 10L, 1L);
        activity.setUpdated(LocalDateTime.of(2026, 11, 2, hour, 0));
        activity.setStatusCode(status);
        return activity;
    }
    @Test void calculatesDevelopmentAndTesting() {
        when(activities.findStatusHistory(10)).thenReturn(List.of(at(9,"in_progress"), at(13,"ready_for_review"), at(15,"done")));
        assertEquals(Duration.ofHours(4), service.timeInDevelopment(task));
        assertEquals(Duration.ofHours(2), service.timeInTesting(task));
    }
    @Test void sumsCompletedDevelopmentCycles() {
        when(activities.findStatusHistory(10)).thenReturn(List.of(at(9,"in_progress"), at(10,"ready_for_review"),
                at(11,"in_progress"), at(13,"ready_for_review"), at(14,"review"), at(15,"test"), at(16,"done")));
        assertEquals(Duration.ofHours(3), service.timeInDevelopment(task));
        assertEquals(Duration.ofHours(3), service.timeInTesting(task));
    }
    @Test void ignoresDuplicateStarts() {
        when(activities.findStatusHistory(10)).thenReturn(List.of(at(9,"in_progress"), at(10,"in_progress"), at(13,"ready_for_review")));
        assertEquals(Duration.ofHours(4), service.timeInDevelopment(task));
        assertEquals(Duration.ZERO, service.timeInTesting(task));
    }
    @Test void ignoresMissingOrIncompleteHistory() {
        when(activities.findStatusHistory(10)).thenReturn(List.of());
        assertEquals(Duration.ZERO, service.timeInDevelopment(task));
        assertEquals(Duration.ZERO, service.timeInTesting(task));
        when(activities.findStatusHistory(10)).thenReturn(List.of(at(9,"in_progress")));
        assertEquals(Duration.ZERO, service.timeInDevelopment(task));
    }
    @Test void discardsCanceledIntervals() {
        when(activities.findStatusHistory(10)).thenReturn(List.of(at(9,"in_progress"), at(10,"canceled"), at(13,"ready_for_review"), at(14,"canceled"), at(15,"done")));
        assertEquals(Duration.ZERO, service.timeInDevelopment(task));
        assertEquals(Duration.ZERO, service.timeInTesting(task));
    }
    @Test void sumsRepeatedTestingCycles() {
        when(activities.findStatusHistory(10)).thenReturn(List.of(at(9,"ready_for_review"), at(10,"done"), at(11,"in_progress"), at(13,"ready_for_review"), at(15,"done")));
        assertEquals(Duration.ofHours(3), service.timeInTesting(task));
    }
    @Test void requiresPersistedTask() {
        assertThrows(IllegalArgumentException.class, () -> service.timeInDevelopment(new Task()));
    }
}
