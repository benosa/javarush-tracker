package com.javarush.jira.bugtracking.task;

import com.javarush.jira.common.BaseRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
public interface TaskRepository extends BaseRepository<Task> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Task t WHERE t.id = :id")
    Optional<Task> findForTagUpdate(long id);

    @Query("SELECT t FROM Task t WHERE t.sprintId =:sprintId ORDER BY t.startpoint DESC")
    List<Task> findAllBySprintId(long sprintId);

    @Query("SELECT t FROM Task t WHERE t.projectId =:projectId AND t.sprintId IS NULL")
    List<Task> findAllByProjectIdAndSprintIsNull(long projectId);

    @Query("SELECT t FROM Task t WHERE t.projectId =:projectId ORDER BY t.startpoint DESC")
    List<Task> findAllByProjectId(long projectId);

    @Query("SELECT t FROM Task t JOIN FETCH t.project LEFT JOIN FETCH t.sprint LEFT JOIN FETCH t.parent WHERE t.id =:id")
    Optional<Task> findFullById(long id);

    @Query("SELECT t.id FROM Task t WHERE t.parentId IN :parentIds")
    List<Long> findChildIds(List<Long> parentIds);

    @Modifying
    @Query("UPDATE Task t SET t.sprintId = :sprintId WHERE t.id IN :ids")
    void setSprintForTasks(java.util.Collection<Long> ids, Long sprintId);
}
