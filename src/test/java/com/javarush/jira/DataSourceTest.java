package com.javarush.jira;

import com.javarush.jira.bugtracking.task.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.*;

class DataSourceTest extends AbstractControllerTest {
    @Autowired DataSource dataSource;
    @Autowired ApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired TaskService tasks;

    @Test void selectsOnlyH2ForTests() throws Exception {
        assertEquals(1, context.getBeansOfType(DataSource.class).size());
        assertTrue(context.containsBean("h2DataSource"));
        assertFalse(context.containsBean("postgresDataSource"));
        try (var connection = dataSource.getConnection()) {
            assertEquals("H2", connection.getMetaData().getDatabaseProductName());
        }
    }

    @Test void movesDescendantsIntoTheSameSprint() {
        jdbc.update("INSERT INTO task(id,title,type_code,status_code,project_id,sprint_id,parent_id) VALUES (101,'Child','task','todo',1,1,1),(102,'Grandchild','task','todo',1,1,101)");
        tasks.changeSprint(1, 2L);
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM task WHERE id IN (1,101,102) AND sprint_id=2", Integer.class));
        assertEquals(1L, jdbc.queryForObject("SELECT sprint_id FROM task WHERE id=2", Long.class));
    }
}
