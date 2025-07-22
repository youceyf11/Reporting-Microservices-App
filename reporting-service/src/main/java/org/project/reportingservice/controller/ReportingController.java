package org.project.reportingservice.controller;

import org.project.reportingservice.dto.EmployeePerformanceDto;
import org.project.reportingservice.dto.MonthlyStatsDto;
import org.project.reportingservice.dto.ReportingResultDto;
import org.project.reportingservice.dto.WeeklyStatsDto;
import org.project.reportingservice.service.ReportingService;
import org.project.reportingservice.response.MonthlyStatsResponse;
import org.project.reportingservice.response.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour les endpoIntegers de reporting
 * Tous les endpoIntegers sont réactifs (retournent Mono/Flux)
 */
@RestController
@RequestMapping("/api/reporting")
public class ReportingController {

    private final ReportingService reportingService;

    private static final Logger log = LoggerFactory.getLogger(ReportingController.class);


    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    /**
     * EndpoInteger principal - Génère le rapport mensuel complet
     *
     * @param projectKey clé du projet JIRA (paramètre requis)
     * @return Mono contenant le rapport complet
     */
    @GetMapping("/monthly")
    public Mono<ResponseEntity<ReportingResultDto>> getMonthlyReport(
            @RequestParam String projectKey) {
        return reportingService.generateMonthlyReport(projectKey)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.ok(new ReportingResultDto("No data found", "", List.of())))
                .onErrorResume(error -> {
                    error.printStackTrace();
                    // Return an empty ReportingResultDto with a message
                    ReportingResultDto emptyResult = new ReportingResultDto("Error occurred", "", List.of());
                    return Mono.just(ResponseEntity.ok(emptyResult));
                });
    }

    /**
     * EndpoInteger pour obtenir le top N des employés les plus actifs
     *
     * @param projectKey clé du projet JIRA
     * @param limit      nombre d'employés à retourner (défaut: 10)
     * @return Mono contenant la liste des top employés
     */
    @GetMapping("/monthly/top")
    public Mono<ResponseEntity<List<EmployeePerformanceDto>>> getTopActiveEmployees(
            @RequestParam String projectKey,
            @RequestParam(defaultValue = "10") Integer limit) {

        return reportingService.getTopActiveEmployees(projectKey, limit)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }

    /**
     * EndpoInteger pour obtenir les statistiques globales du mois
     *
     * @param projectKey clé du projet JIRA
     * @return Mono contenant les statistiques (total heures, nombre employés)
     */
   /*  @GetMapping("/monthly/stats")
    public Mono<ResponseEntity<MonthlyStatsResponse>> getMonthlyStatistics(
            @RequestParam String projectKey) {
        return reportingService.getMonthlyStatistics(projectKey)
                .map(this::createStatsResponse)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.ok(new MonthlyStatsResponse(0.0, 0, 0.0)))
                .onErrorResume(error -> {
                    log.error("Erreur interne:", error);
                    return Mono.just(ResponseEntity.status(500)
                            .body(new MonthlyStatsResponse(0.0, 0, 0.0)));

                });

    } */
    /* @GetMapping("/monthly/stats")
    public Mono<ResponseEntity<MonthlyStatsResponse>> getMonthlyStatistics(
            @RequestParam String projectKey) {

        return reportingService.getMonthlyStatistics(projectKey)
                .map(this::createStatsResponse)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Erreur lors de la génération des statistiques pour le projet {}: {}",
                            projectKey, error.getMessage(), error);

                    // Réponse d'erreur avec des valeurs par défaut correctes
                    MonthlyStatsResponse errorResponse = new MonthlyStatsResponse(0.0, 0, 0.0);
                    return Mono.just(ResponseEntity.status(500).body(errorResponse));
                })
                .defaultIfEmpty(ResponseEntity.ok(new MonthlyStatsResponse(0.0, 0, 0.0)));
    } */

    @GetMapping("/monthly/stats")
    public Mono<ResponseEntity<MonthlyStatsResponse>> getMonthlyStatistics(
            @RequestParam String projectKey) {

        log.info("=== DEBUG: Début getMonthlyStatistics pour projet: {}", projectKey);

        return reportingService.getMonthlyStatistics(projectKey)
                .doOnNext(stats -> {
                    log.info("=== DEBUG: Stats reçues du service: totalHours={}, employeeCount={}",
                            stats.getT1(), stats.getT2());
                })
                .map(stats -> {
                    log.info("=== DEBUG: Avant createStatsResponse");
                    MonthlyStatsResponse response = this.createStatsResponse(stats);
                    log.info("=== DEBUG: Après createStatsResponse: {}", response);
                    return response;
                })
                .map(response -> {
                    log.info("=== DEBUG: Création ResponseEntity OK");
                    return ResponseEntity.ok(response);
                })
                .doOnError(error -> {
                    log.error("=== DEBUG: ERREUR CAPTUREE - Type: {}, Message: {}",
                            error.getClass().getSimpleName(), error.getMessage());
                    log.error("=== DEBUG: Stack trace complète:", error);
                })
                .onErrorResume(error -> {
                    log.error("=== DEBUG: Dans onErrorResume");
                    MonthlyStatsResponse errorResponse = new MonthlyStatsResponse(0.0, 0, 0.0);
                    return Mono.just(ResponseEntity.status(500).body(errorResponse));
                });
    }

   /* @GetMapping("/test-response")
    public Mono<ResponseEntity<MonthlyStatsResponse>> testResponse() {
        MonthlyStatsResponse response = new MonthlyStatsResponse(10.0, 2, 5.0);
        return Mono.just(ResponseEntity.ok(response));
    } */

    /**
     * EndpoInteger pour vérifier la santé du service
     *
     * @return Mono contenant le statut du service
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<HealthResponse>> healthCheck() {
        return Mono.just(ResponseEntity.ok(new HealthResponse("UP", "Reporting service is healthy")));
    }

    /**
     * Get weekly statistics for a specific employee
     */
    @GetMapping("/weekly/stats/{projectKey}/{assignee}")
    public Mono<ResponseEntity<WeeklyStatsDto>> getEmployeeWeeklyStats(
            @PathVariable String projectKey,
            @PathVariable String assignee) {

        return reportingService.getEmployeeWeeklyStats(projectKey, assignee)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Get monthly statistics for a specific employee
     */
    @GetMapping("/monthly/stats/{projectKey}/{assignee}")
    public Mono<ResponseEntity<MonthlyStatsDto>> getEmployeeMonthlyStats(
            @PathVariable String projectKey,
            @PathVariable String assignee,
            @RequestParam(defaultValue = "160.0") Double expectedHours) {

        return reportingService.getEmployeeMonthlyStats(projectKey, assignee, expectedHours)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/monthly/detailed/{projectKey}")
    public Mono<Map<String, Map<Integer, Double>>> getDetailedMonthlyStatistics(@PathVariable String projectKey) {
        return reportingService.getDetailedMonthlyStatistics(projectKey);
    }

    /**
     * Crée la réponse des statistiques à partir du tuple
     *
     * @param stats tuple contenant (total heures, nombre employés)
     * @return objet de réponse des statistiques
     */
   /* private MonthlyStatsResponse createStatsResponse(Tuple2<Double, Integer> stats) {
        // Ajoutez une vérification null
        if (stats == null) {
            return new MonthlyStatsResponse(0.0, 0, 0.0);
        }

        Double totalHours = stats.getT1() != null ? stats.getT1() : 0.0;
        Integer employeeCount = stats.getT2() != null ? stats.getT2() : 0;
        Double average = employeeCount > 0 ? totalHours / employeeCount : 0.0;

        return new MonthlyStatsResponse(totalHours, employeeCount, average);
    } */
    private MonthlyStatsResponse createStatsResponse(Tuple2<Double, Integer> stats) {
        try {
            Double totalHours = stats.getT1() != null ? stats.getT1() : 0.0;
            Integer employeeCount = stats.getT2() != null ? stats.getT2() : 0;
            Double averagePerEmployee = employeeCount > 0 ? totalHours / employeeCount : 0.0;

            return new MonthlyStatsResponse(totalHours, employeeCount, averagePerEmployee);
        } catch (Exception e) {
            log.error("Erreur lors de la création de la réponse des statistiques:", e);
            return new MonthlyStatsResponse(0.0, 0, 0.0);
        }

    }
}