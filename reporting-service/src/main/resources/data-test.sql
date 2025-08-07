DROP TABLE IF EXISTS jira_issue CASCADE;

CREATE TABLE jira_issue (
    id                VARCHAR(255) PRIMARY KEY,
    issue_key         VARCHAR(255) UNIQUE NOT NULL,
    project_key       VARCHAR(255) NOT NULL,
    self_url          VARCHAR(500),
    summary           TEXT,
    issue_type        VARCHAR(255),
    status            VARCHAR(255),
    priority          VARCHAR(255),
    resolution        VARCHAR(255),
    assignee          VARCHAR(255),
    assignee_email    VARCHAR(255),
    reporter          VARCHAR(255),
    reporter_email    VARCHAR(255),
    created           VARCHAR(255),
    updated           VARCHAR(255),
    resolved          VARCHAR(255),
    time_spent_seconds BIGINT,
    organization      VARCHAR(255),
    classification    VARCHAR(255),
    "entity"         VARCHAR(255),
    issue_quality     VARCHAR(255),
    medium            VARCHAR(255),
    tts_days          DECIMAL(10,2),
    site              VARCHAR(255),
    issue_month       VARCHAR(255),
    quota_per_project VARCHAR(255)
);

-- SCRUM : cas standard
INSERT INTO jira_issue VALUES
  ('ID-1', 'SCRUM-1', 'SCRUM', NULL, 'Login fails', 'Bug',
   'Closed', 'High', 'Fixed', 'alice', 'alice@corp', 'bob', 'bob@corp',
   '2025-08-01', '2025-08-02', '2025-08-05',  72000,
   'Logistics', 'Level-1', 'Mobile', 'Valid', 'Email', 4.0,
   'LKS', '08/2025', 'Quota');

-- SCRUM : toujours ouvert
INSERT INTO jira_issue VALUES
  ('ID-2', 'SCRUM-2', 'SCRUM', NULL, 'Add “remember me”', 'Story',
   'In Progress', 'Low', 'WIP', 'charles', 'charles@corp', 'david', 'david@corp',
   '2025-08-10', '2025-08-12', '2025-08-15', 36000,
   'Logistics', 'Level-2', 'Web', 'WIP', 'Chat', 2.0,
   'LKS', '08/2025', 'Quota');

-- DEVOPS : autre projet, autre mois
INSERT INTO jira_issue VALUES
  ('ID-3', 'DEV-1', 'DEVOPS', NULL, 'K8s upgrade', 'Task',
   'Closed', 'Medium', 'Done', 'eve', 'eve@corp', 'frank', 'frank@corp',
   '2025-06-15', '2025-08-20', '2025-08-25', 36000,
   'Infrastructure', 'Level-1', 'Backend', 'Valid', 'Email', 10.2,
   'LHR', '08/2025', 'Ops');