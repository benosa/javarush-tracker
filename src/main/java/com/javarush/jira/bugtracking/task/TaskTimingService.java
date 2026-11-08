package com.javarush.jira.bugtracking.task;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskTimingService {
    private final ActivityRepository activities;

    public Duration timeInDevelopment(Task task) {
        return completedIntervals(task, "in_progress", "ready_for_review", Set.of("canceled", "done"));
    }

    public Duration timeInTesting(Task task) {
        return completedIntervals(task, "ready_for_review", "done", Set.of("canceled", "in_progress"));
    }

    private Duration completedIntervals(Task task, String from, String to, Set<String> interruptions) {
        Duration total = Duration.ZERO;
        LocalDateTime start = null;
        for (Activity activity : activities.findStatusHistory(task.id())) {
            if (activity.getUpdated() == null) continue;
            String status = activity.getStatusCode();
            if (from.equals(status) && start == null) {
                start = activity.getUpdated();
            } else if (to.equals(status) && start != null) {
                if (!activity.getUpdated().isBefore(start)) {
                    total = total.plus(Duration.between(start, activity.getUpdated()));
                }
                start = null;
            } else if (interruptions.contains(status)) {
                start = null;
            }
        }
        // An unfinished interval has no end timestamp yet.
        return total;
    }
}
