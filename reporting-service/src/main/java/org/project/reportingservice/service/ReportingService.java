package org.project.reportingservice.service;

import org.project.reportingservice.dto.EmployeePerformanceDto;
import org.project.reportingservice.dto.ReportingResultDto;
import org.project.reportingservice.dto.IssueSimpleDto;
import org.project.reportingservice.utils.TimeUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.YearMonth;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service principal pour la génération des rapports mensuels
 * Utilise WebFlux pour un traitement réactif complet
 */
@Service
public class ReportingService {

    private final WebClient jiraFetchWebClient;

    public ReportingService(@Qualifier("jiraFetchWebClient") WebClient jiraFetchWebClient) {
        this.jiraFetchWebClient = jiraFetchWebClient;
    }

    /**
     * Génère le rapport mensuel des employés
     * 
     * @param projectKey clé du projet JIRA
     * @return Mono contenant le rapport complet
     */
    public Mono<ReportingResultDto> generateMonthlyReport(String projectKey) {
        return fetchIssuesFromJiraService(projectKey)
                .filter(this::isResolvedInCurrentMonth)
                .filter(issue -> issue.getAssignee() != null && !issue.getAssignee().trim().isEmpty())
                .groupBy(IssueSimpleDto::getAssignee)
                .flatMap(this::processEmployeeGroup)
                .collectList()
                .map(this::createReportWithRanking);
    }

    /**
     * Récupère les issues depuis le jira-fetch-service
     * 
     * @param projectKey clé du projet
     * @return Flux d'issues
     */
    private Flux<IssueSimpleDto> fetchIssuesFromJiraService(String projectKey) {
        String jql = String.format("project='%s'", projectKey);

        return jiraFetchWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/jira/projects/{projectKey}/issues")
                        .queryParam("jql", jql)
                        .build(projectKey))
                .retrieve()
                .bodyToFlux(IssueSimpleDto.class)
                .onErrorResume(throwable -> {
                    System.err.println("Erreur lors de la récupération des issues: " + throwable.getMessage());
                    return Flux.empty();
                });
    }

    /**
     * Vérifie si une issue est résolue dans le mois courant
     * 
     * @param issue issue à vérifier
     * @return true si résolue dans le mois courant
     */
    private boolean isResolvedInCurrentMonth(IssueSimpleDto issue) {
        return issue.isResolved() && TimeUtils.isInCurrentMonth(issue.getResolved());
    }

    /**
     * Traite un groupe d'issues par employé et calcule les métriques
     * 
     * @param groupedFlux flux groupé par assignee
     * @return Mono contenant le rapport de l'employé
     */
    private Mono<EmployeePerformanceDto> processEmployeeGroup(Flux<IssueSimpleDto> groupedFlux) {
        return groupedFlux
                .collectList()
                .map(this::calculateEmployeeMetrics);
    }

    /**
     * Calcule les métriques d'un employé à partir de ses issues
     * 
     * @param issues liste des issues de l'employé
     * @return rapport de l'employé
     */
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
                .mapToDouble(issue -> TimeUtils.calculateHoursDifference(
                        issue.getCreated(), issue.getResolved()))
                .average()
                .orElse(0.0);

        // Nombre d'issues résolues
        Integer resolvedIssuesCount = issues.size();

        YearMonth currentMonth = YearMonth.now();
        double expectedHoursThisMonth = TimeUtils.getExpectedHoursForMonth(currentMonth, 8.0); // 8 heures par jour

        // Calcul de la performance
        double performancePercentage = TimeUtils.calculatePerformancePercentage(totalHoursWorked, expectedHoursThisMonth);

        // Niveau de performance
        String performanceLevel = TimeUtils.determinePerformanceLevel(performancePercentage, 0.9, 0.7, 0.5);

        return new EmployeePerformanceDto(
            assignee,
            TimeUtils.roundToTwoDecimals(totalHoursWorked),
            resolvedIssuesCount,
            TimeUtils.roundToTwoDecimals(averageResolutionTimeHours),
            TimeUtils.roundToTwoDecimals(performancePercentage),
            performanceLevel,
            expectedHoursThisMonth
        );
    }

    /**
     * Crée le rapport final avec classement des employés
     * 
     * @param employeeReports liste des rapports d'employés
     * @return rapport final
     */
    private ReportingResultDto createReportWithRanking(List<EmployeePerformanceDto> employeeReports) {
        // Tri par heures travaillées (décroissant) pour le classement
        List<EmployeePerformanceDto> sortedReports = employeeReports.stream()
                .sorted((e1, e2) -> Double.compare(e2.getTotalHoursWorked(), e1.getTotalHoursWorked()))
                .toList();

        // Attribution des rangs
        AtomicInteger rank = new AtomicInteger(1);
        sortedReports.forEach(report -> report.setRanking(rank.getAndIncrement()));

        return new ReportingResultDto(
            TimeUtils.getCurrentMonth(LocalDateTime.now()),
            TimeUtils.getCurrentYear(),
            sortedReports
        );
    }

    /**
     * Méthode utilitaire pour obtenir le top N des employés les plus actifs
     * 
     * @param projectKey clé du projet
     * @param topN nombre d'employés à retourner
     * @return Mono contenant les top employés
     */
    public Mono<List<EmployeePerformanceDto>> getTopActiveEmployees(String projectKey, Integer topN) {
        return generateMonthlyReport(projectKey)
                .map(report -> report.getEmployeeReports().stream()
                        .limit(topN)
                        .toList());
    }

    /**
     * Obtient les statistiques globales du mois
     * 
     * @param projectKey clé du projet
     * @return Mono contenant un tuple (total heures, nombre employés)
     */
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
}