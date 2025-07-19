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
                .map(report -> {
                    List<EmployeePerformanceDto> rankings = report.getEmployeeRankings() != null ? report.getEmployeeRankings() : List.of();
                    return rankings.stream()
                            .limit(topN != null ? topN : 0)
                            .toList();
                });
    }

   /* @Override
    public Mono<Tuple2<Double, Integer>> getMonthlyStatistics(String projectKey) {
        return generateMonthlyReport(projectKey)
                .map(report -> {
                    List<EmployeePerformanceDto> rankings = report.getEmployeeRankings() != null ? report.getEmployeeRankings() : List.of();
                    double totalHours = rankings.stream()
                            .mapToDouble(e -> e.getTotalHoursWorked() != null ? e.getTotalHoursWorked() : 0.0)
                            .sum();
                    Integer employeeCount = report.getTotalEmployees() != null ? report.getTotalEmployees() : 0;
                    return reactor.util.function.Tuples.of(totalHours, employeeCount);
                });
    } */
  /* @Override
   public Mono<Tuple2<Double, Integer>> getMonthlyStatistics(String projectKey) {
       return generateMonthlyReport(projectKey)
               .map(report -> {
                   try {
                       List<EmployeePerformanceDto> rankings = report.getEmployeeRankings();

                       // Vérification de null safety
                       if (rankings == null || rankings.isEmpty()) {
                           return reactor.util.function.Tuples.of(0.0, 0);
                       }

                       // Calcul cohérent basé sur les mêmes données
                       double totalHours = rankings.stream()
                               .mapToDouble(e -> e.getTotalHoursWorked() != null ? e.getTotalHoursWorked() : 0.0)
                               .sum();

                       int employeeCount = rankings.size(); // Utiliser la taille de la liste plutôt que report.getTotalEmployees()

                       return reactor.util.function.Tuples.of(totalHours, employeeCount);

                   } catch (Exception e) {
                       System.err.println("[ReportingService] Erreur dans getMonthlyStatistics: " + e.getMessage());
                       e.printStackTrace();
                       return reactor.util.function.Tuples.of(0.0, 0);
                   }
               })
               .onErrorReturn(reactor.util.function.Tuples.of(0.0, 0)); // Fallback en cas d'erreur
   }*/
   @Override
   public Mono<Tuple2<Double, Integer>> getMonthlyStatistics(String projectKey) {
       System.out.println("=== DEBUG SERVICE: Début getMonthlyStatistics pour projet: " + projectKey);

       return generateMonthlyReport(projectKey)
               .doOnNext(report -> {
                   System.out.println("=== DEBUG SERVICE: Report généré, employeeRankings: " +
                           (report.getEmployeeRankings() != null ? report.getEmployeeRankings().size() : "null"));
               })
               .map(report -> {
                   try {
                       System.out.println("=== DEBUG SERVICE: Dans map, début calcul");

                       List<EmployeePerformanceDto> rankings = report.getEmployeeRankings();
                       System.out.println("=== DEBUG SERVICE: Rankings récupérés: " + (rankings != null ? rankings.size() : "null"));

                       if (rankings == null || rankings.isEmpty()) {
                           System.out.println("=== DEBUG SERVICE: Rankings vide, retour (0.0, 0)");
                           return reactor.util.function.Tuples.of(0.0, 0);
                       }

                       double totalHours = rankings.stream()
                               .mapToDouble(e -> {
                                   Double hours = e.getTotalHoursWorked();
                                   System.out.println("=== DEBUG SERVICE: Employee hours: " + hours);
                                   return hours != null ? hours : 0.0;
                               })
                               .sum();

                       int employeeCount = rankings.size();

                       System.out.println("=== DEBUG SERVICE: Calculs finaux - totalHours: " + totalHours +
                               ", employeeCount: " + employeeCount);

                       Tuple2<Double, Integer> result = reactor.util.function.Tuples.of(totalHours, employeeCount);
                       System.out.println("=== DEBUG SERVICE: Tuple créé avec succès");

                       return result;

                   } catch (Exception e) {
                       System.err.println("=== DEBUG SERVICE: EXCEPTION dans map: " + e.getMessage());
                       e.printStackTrace();
                       return reactor.util.function.Tuples.of(0.0, 0);
                   }
               })
               .doOnError(error -> {
                   System.err.println("=== DEBUG SERVICE: ERREUR dans getMonthlyStatistics: " + error.getMessage());
                   error.printStackTrace();
               });
   }

    // --- Internal business logic methods ---

    private boolean isResolvedInCurrentMonth(IssueSimpleDto issue) {
        return issue != null && issue.isResolved() && timeUtils.isInCurrentMonth(issue.getResolved());
    }

    private Mono<EmployeePerformanceDto> processEmployeeGroup(Flux<IssueSimpleDto> groupedFlux) {
        return groupedFlux
                .collectList()
                .map(this::calculateEmployeeMetrics);
    }

    private EmployeePerformanceDto calculateEmployeeMetrics(List<IssueSimpleDto> issues) {
        if (issues == null || issues.isEmpty()) {
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

        String assignee = issues.get(0).getAssignee() != null ? issues.get(0).getAssignee() : "Unknown";

        // Calcul du temps total travaillé (en heures)
        double totalHoursWorked = issues.stream()
                .mapToDouble(issue -> issue != null && issue.getTimeSpentSeconds() != null ? issue.getTimeSpentSeconds() / 3600.0 : 0.0)
                .sum();

        // Calcul de la moyenne du temps de résolution
        double averageResolutionTimeHours = issues.stream()
                .filter(issue -> issue != null && issue.getCreated() != null && issue.getResolved() != null)
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
                List.of()
            );
        }
        // Tri par heures travaillées (décroissant) pour le classement
        List<EmployeePerformanceDto> sortedReports = employeeReports.stream()
                .sorted((e1, e2) -> Double.compare(
                        e2.getTotalHoursWorked() != null ? e2.getTotalHoursWorked() : 0.0,
                        e1.getTotalHoursWorked() != null ? e1.getTotalHoursWorked() : 0.0))
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