package com.javarush.jira.bugtracking.task;

import com.javarush.jira.common.error.IllegalRequestDataException;
import com.javarush.jira.common.util.Util;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class TaskTagService {
    private final TaskRepository repository;

    @Transactional(readOnly = true)
    public Set<String> getTags(long taskId) {
        return new TreeSet<>(repository.getExisted(taskId).getTags());
    }

    @Transactional
    public Set<String> addTag(long taskId, String tag) {
        String value = normalize(tag);
        Task task = Util.checkExist(taskId, repository.findForTagUpdate(taskId));
        task.getTags().add(value);
        return new TreeSet<>(task.getTags());
    }

    @Transactional
    public void removeTag(long taskId, String tag) {
        String value = normalize(tag);
        Task task = Util.checkExist(taskId, repository.findForTagUpdate(taskId));
        task.getTags().remove(value);
    }

    private String normalize(String tag) {
        if (tag == null) throw new IllegalRequestDataException("Tag is required");
        String value = tag.strip();
        if (value.length() < 2 || value.length() > 32 || !Jsoup.isValid(value, Safelist.none())) {
            throw new IllegalRequestDataException("Tag must contain 2–32 characters without HTML");
        }
        return value;
    }
}
