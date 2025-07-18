package org.project.jirafetchservice.jiraApi;

import java.util.List;

public class JiraSearchResponse {
    private List<JiraIssueApiResponse> issues;
    private int total;

    public List<JiraIssueApiResponse> getIssues() {
        return issues;
    }

    public void setIssues(List<JiraIssueApiResponse> issues) {
        this.issues = issues;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}