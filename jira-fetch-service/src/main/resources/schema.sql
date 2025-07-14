CREATE TABLE IF NOT EXISTS jira_issue (
    id VARCHAR(255) PRIMARY KEY,
    issue_key VARCHAR(255) UNIQUE NOT NULL,
    project_key VARCHAR(255) NOT NULL,
    self_url VARCHAR(500),
    summary TEXT,
    issue_type VARCHAR(255),
    status VARCHAR(255),
    priority VARCHAR(255),
    resolution VARCHAR(255),
    assignee VARCHAR(255),
    assignee_email VARCHAR(255),
    reporter VARCHAR(255),
    reporter_email VARCHAR(255),
    created VARCHAR(255),
    updated VARCHAR(255),
    resolved VARCHAR(255),
    time_spent_seconds BIGInteger,
    organization VARCHAR(255),
    classification VARCHAR(255),
    entity VARCHAR(255),
    issue_quality VARCHAR(255),
    medium VARCHAR(255),
    tts_days DECIMAL(10,2),
    site VARCHAR(255),
    issue_month VARCHAR(255),
    quota_per_project VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_jira_issue_project_key ON jira_issue(project_key);
CREATE INDEX IF NOT EXISTS idx_jira_issue_status ON jira_issue(status);
CREATE INDEX IF NOT EXISTS idx_jira_issue_assignee ON jira_issue(assignee);