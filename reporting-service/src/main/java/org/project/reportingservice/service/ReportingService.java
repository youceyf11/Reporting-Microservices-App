package org.project.reportingservice.service;

import org.project.reportingservice.dto.EmployeePerformanceDto;
import org.project.reportingservice.dto.ReportingResultDto;
import org.project.reportingservice.dto.IssueSimpleDto;
import org.project.reportingservice.iservice.IReportingService;
import org.project.reportingservice.iservice.IJiraClient;
import org.project.reportingservice.iservice.ITimeUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.YearMonth;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ReportingService implements IReportingService {

    private final IJiraClient jiraClient;
    private final ITimeUtils timeUtils;

    
    public ReportingService(IJiraClient jiraClient, ITimeUtils timeUtils) {
        this.jiraClient = jiraClient;
        this.timeUtils = timeUtils;
    }

    @Override
    public Mono<ReportingResultDto> generateMonthlyReport(String projectKey) {
        return jiraClient.fetchProjectIssues(projectKey, 100)
                .doOnNext(issue -> System.out.println("[ReportingService] Received issue: " + issue))
                .doOnError(error -> System.err.println("[ReportingService] Error: " + error))
                .filter(this::isResolvedInCurrentMonth)
                .filter(issue -> issue.getAssignee() != null && !issue.getAssignee().trim().isEmpty())
                .groupBy(IssueSimpleDto::getAssignee)
                .flatMap(this::processEmployeeGroup)
                .collectList()
                .doOnError(error -> System.err.println("[ReportingService] Final error: " + error))
                .map(this::createReportWithRanking);
    }

    @Override
    public Mono<List<EmployeePerformanceDto>> getTopActiveEmployees(String projectKey, Integer topN) {
        return generateMonthlyReport(projectKey)
                .map(report -> report.getEmployeeReports().stream()
                        .limit(topN)
                        .toList());
    }

    @Override
    public Mono<Tuple2<Double, Integer>> getMonthlyStatistics(String projectKey) {
        return generateMonthlyReport(projectKey)
                .map(report -> {
                    double totalHours = report.getEmployeeReports().stream()
                            .mapToDouble(EmployeePerformanceDto::getTotalHoursWorked)
                            .sum();
                    Integer employeeCount = report.getTotalEmployees();
                    return reactor.util.function.Tuples.of(totalHours, employeeCount);
                });
    }

    // --- Internal business logic methods ---

    private boolean isResolvedInCurrentMonth(IssueSimpleDto issue) {
        return issue.isResolved() && timeUtils.isInCurrentMonth(issue.getResolved());
    }

    private Mono<EmployeePerformanceDto> processEmployeeGroup(Flux<IssueSimpleDto> groupedFlux) {
        return groupedFlux
                .collectList()
                .map(this::calculateEmployeeMetrics);
    }

    private EmployeePerformanceDto calculateEmployeeMetrics(List<IssueSimpleDto> issues) {
        if (issues.isEmpty()) {
            return new EmployeePerformanceDto(
                "Unknown",
                0.0,
                0,
                0.0,
                0.0,
                "UNKNOWN",
                0.0
            );
        }

        String assignee = issues.get(0).getAssignee();

        // Calcul du temps total travaillé (en heures)
        double totalHoursWorked = issues.stream()
                .mapToDouble(issue -> issue.getTimeSpentSeconds() / 3600.0)
                .sum();

        // Calcul de la moyenne du temps de résolution
        double averageResolutionTimeHours = issues.stream()
                .filter(issue -> issue.getCreated() != null && issue.getResolved() != null)
                .mapToDouble(issue -> timeUtils.calculateHoursDifference(
                        issue.getCreated(), issue.getResolved()))
                .average()
                .orElse(0.0);

        // Nombre d'issues résolues
        Integer resolvedIssuesCount = issues.size();

        YearMonth currentMonth = YearMonth.now();
        double expectedHoursThisMonth = timeUtils.getExpectedHoursForMonth(currentMonth, 8.0); // 8 heures par jour

        // Calcul de la performance
        double performancePercentage = timeUtils.calculatePerformancePercentage(totalHoursWorked, expectedHoursThisMonth);

        // Niveau de performance
        String performanceLevel = timeUtils.determinePerformanceLevel(performancePercentage, 0.9, 0.7, 0.5);

        return new EmployeePerformanceDto(
            assignee,
            timeUtils.roundToTwoDecimals(totalHoursWorked),
            resolvedIssuesCount,
            timeUtils.roundToTwoDecimals(averageResolutionTimeHours),
            timeUtils.roundToTwoDecimals(performancePercentage),
            performanceLevel,
            expectedHoursThisMonth
        );
    }

    private ReportingResultDto createReportWithRanking(List<EmployeePerformanceDto> employeeReports) {
        if (employeeReports == null || employeeReports.isEmpty()) {
            // Return an empty or default ReportingResultDto
            return new ReportingResultDto(
                timeUtils.getCurrentMonth(LocalDateTime.now()),
                timeUtils.getCurrentYear(),
                List.of() // or Collections.emptyList()
            );
        }
        // Tri par heures travaillées (décroissant) pour le classement
        List<EmployeePerformanceDto> sortedReports = employeeReports.stream()
                .sorted((e1, e2) -> Double.compare(e2.getTotalHoursWorked(), e1.getTotalHoursWorked()))
                .toList();

        // Attribution des rangs
        AtomicInteger rank = new AtomicInteger(1);
        sortedReports.forEach(report -> report.setRanking(rank.getAndIncrement()));

        return new ReportingResultDto(
            timeUtils.getCurrentMonth(LocalDateTime.now()),
            timeUtils.getCurrentYear(),
            sortedReports
        );
    }
}