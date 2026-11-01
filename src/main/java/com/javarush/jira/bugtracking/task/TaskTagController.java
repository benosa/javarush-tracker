package com.javarush.jira.bugtracking.task;

import com.javarush.jira.common.util.validation.NoHtml;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Set;

@RestController
@RequestMapping("/api/tasks/{taskId}/tags")
@RequiredArgsConstructor
public class TaskTagController {
    private final TaskTagService service;

    @GetMapping
    public Set<String> get(@PathVariable long taskId) {
        return service.getTags(taskId);
    }

    @PostMapping
    public Set<String> add(@PathVariable long taskId, @Valid @RequestBody TagRequest request) {
        return service.addTag(taskId, request.tag());
    }

    @DeleteMapping("/{tag}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable long taskId, @PathVariable String tag) {
        service.removeTag(taskId, tag);
    }

    public record TagRequest(@NotBlank @Size(max = 32) @NoHtml String tag) { }
}
