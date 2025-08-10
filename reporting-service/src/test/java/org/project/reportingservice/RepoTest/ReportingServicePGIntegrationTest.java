package org.project.reportingservice.RepoTest;

import org.junit.jupiter.api.*;
import org.project.reportingservice.dto.IssueSimpleDto;
import org.project.reportingservice.service.JiraClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReportingServicePGIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("reporting")
            .withUsername("test")
            .withPassword("test")
            .withReuse(false);

    @DynamicPropertySource
    static void registerPgProps(DynamicPropertyRegistry registry) {
        String r2dbcUrl = String.format("r2dbc:postgresql://%s:%d/%s", postgres.getHost(), postgres.getFirstMappedPort(), postgres.getDatabaseName());
        registry.add("spring.r2dbc.url", () -> r2dbcUrl);
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Autowired
    DatabaseClient databaseClient;

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    JiraClient jiraClient;

    @AfterAll
    static void tearDown() {
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
            postgres.close();
        }
    }

    @BeforeEach 
    void setUpSchema() {
        // Schema matching production jira_issue table structure
        databaseClient.sql("CREATE TABLE IF NOT EXISTS jira_issue (" +
                "id VARCHAR(255) PRIMARY KEY, " +
                "issue_key VARCHAR(255) UNIQUE NOT NULL, " +
                "project_key VARCHAR(255) NOT NULL, " +
                "self_url VARCHAR(500), " +
                "summary TEXT, " +
                "issue_type VARCHAR(255), " +
                "status VARCHAR(255), " +
                "priority VARCHAR(255), " +
                "resolution VARCHAR(255), " +
                "assignee VARCHAR(255), " +
                "assignee_email VARCHAR(255), " +
                "reporter VARCHAR(255), " +
                "reporter_email VARCHAR(255), " +
                "created TIMESTAMP, " +
                "updated TIMESTAMP, " +
                "resolved TIMESTAMP, " +
                "time_spent_seconds BIGINT, " +
                "organization VARCHAR(255), " +
                "classification VARCHAR(255), " +
                "entity VARCHAR(255), " +
                "issue_quality VARCHAR(255), " +
                "medium VARCHAR(255), " +
                "tts_days DECIMAL(10,2), " +
                "site VARCHAR(255), " +
                "issue_month VARCHAR(255), " +
                "quota_per_project VARCHAR(255))").then().block();
    }

    @Test
    @Order(1)
    void saveAndRetrieveIssue_CRUD() {
        String issueId = "TEST-1";
        Integer timeSpentSeconds = 18000; // 5 hours * 3600 seconds
        
        databaseClient.sql("INSERT INTO jira_issue (id, issue_key, project_key, assignee, time_spent_seconds, created) " +
                "VALUES (:id, :issueKey, 'PROJ', 'Alice', :timeSpent, NOW())")
                .bind("id", issueId)
                .bind("issueKey", issueId)
                .bind("timeSpent", timeSpentSeconds)
                .then()
                .block();

        Long retrievedSeconds = databaseClient.sql("SELECT time_spent_seconds FROM jira_issue WHERE id = :id")
                .bind("id", issueId)
                .map(row -> row.get("time_spent_seconds", Long.class))
                .one()
                .block();

        assertThat(retrievedSeconds).isEqualTo(18000L);
    }

    @Test
    @Order(2)
    void monthlyStatsEndpoint_returnsAggregateFromDB() {
        // Mock JiraClient to return test data
        IssueSimpleDto issue1 = new IssueSimpleDto("TEST-2", "Bob", 28800L, "2025-08-15T10:30:00");
        IssueSimpleDto issue2 = new IssueSimpleDto("TEST-3", "Charlie", 43200L, "2025-08-15T14:30:00");
        when(jiraClient.fetchProjectIssues(anyString(), anyInt())).thenReturn(Flux.fromIterable(Arrays.asList(issue1, issue2)));

        webTestClient.get()
                .uri("/api/reporting/monthly/stats?projectKey=PROJ")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalHoursWorked").isEqualTo(20.0)
                .jsonPath("$.totalEmployees").isEqualTo(2);
    }

    @Test
    @Order(3)
    void constraintViolation_throwsError() {
        // NOT NULL violation for project_key and issue_key
        Mono<Void> insert = databaseClient.sql("INSERT INTO jira_issue (id, assignee, time_spent_seconds, created) " +
                "VALUES ('TEST-4', 'Eve', 10800, NOW())").then();

        Assertions.assertThrows(Exception.class, insert::block);
    }
}
