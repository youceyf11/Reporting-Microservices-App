package org.project.reportingservice.service;

import org.project.reportingservice.dto.IssueSimpleDto;
import org.project.reportingservice.iservice.IJiraClient;
import org.springframework.context.annotation.Profile;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * IJiraClient implementation used only when the <code>testdata</code> Spring profile is active.
 * Instead of calling the remote jira-fetch-service, it simply queries the local in-memory H2
 * database that is pre-populated by <code>data-test.sql</code>.
 */
@Service
@Profile("testdata")
public class TestDataJiraClient implements IJiraClient {

    private final DatabaseClient dbClient;

    public TestDataJiraClient(DatabaseClient dbClient) {
        this.dbClient = dbClient;
    }

    @Override
    public Flux<IssueSimpleDto> fetchProjectIssues(String projectKey, int limit) {
        String sql = "SELECT * FROM jira_issue WHERE project_key = :pk LIMIT :lim";
        return dbClient.sql(sql)
                .bind("pk", projectKey)
                .bind("lim", limit)
                .map((row, meta) -> IssueSimpleDto.builder()
                        .issueKey(row.get("issue_key", String.class))
                        .summary(row.get("summary", String.class))
                        .issueType(row.get("issue_type", String.class))
                        .status(row.get("status", String.class))
                        .priority(row.get("priority", String.class))
                        .resolution(row.get("resolution", String.class))
                        .assignee(row.get("assignee", String.class))
                        .reporter(row.get("reporter", String.class))
                        .organization(row.get("organization", String.class))
                        .created(parseDate(row.get("created", String.class)))
                        .updated(parseDate(row.get("updated", String.class)))
                        .resolved(parseDate(row.get("resolved", String.class)))
                        .timeSpentSeconds(row.get("time_spent_seconds", Long.class))
                        .classification(row.get("classification", String.class))
                        .entity(row.get("entity", String.class))
                        .issueQuality(row.get("issue_quality", String.class))
                        .medium(row.get("medium", String.class))
                        .ttsDays(row.get("tts_days", Double.class))
                        .site(row.get("site", String.class))
                        .month(row.get("issue_month", String.class))
                        .quotaPerProject(row.get("quota_per_project", String.class))
                        .build())
                .all();
    }

    @Override
    public Flux<IssueSimpleDto> fetchIssuesByJql(String jql, int limit) {
        // For the test profile we do not parse JQL – return an empty Flux or extend as needed.
        return Flux.empty();
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null) return null;
        try {
            if (dateStr.length() == 10) { // yyyy-MM-dd
                return LocalDate.parse(dateStr).atStartOfDay();
            }
            return LocalDateTime.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }
}
