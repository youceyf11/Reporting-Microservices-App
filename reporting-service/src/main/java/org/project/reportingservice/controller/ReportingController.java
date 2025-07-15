package org.project.reportingservice.controller;

import org.project.reportingservice.dto.EmployeePerformanceDto;
import org.project.reportingservice.dto.ReportingResultDto;
import org.project.reportingservice.service.ReportingService;
import org.project.reportingservice.response.MonthlyStatsResponse;
import org.project.reportingservice.response.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;

/**
 * Contrôleur REST pour les endpoIntegers de reporting
 * Tous les endpoIntegers sont réactifs (retournent Mono/Flux)
 */
@RestController
@RequestMapping("/api/reporting")
public class ReportingController {
    
    private final ReportingService reportingService;
    
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
     * @param limit nombre d'employés à retourner (défaut: 10)
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
    @GetMapping("/monthly/stats")
    public Mono<ResponseEntity<MonthlyStatsResponse>> getMonthlyStatistics(
            @RequestParam String projectKey) {
        return reportingService.getMonthlyStatistics(projectKey)
                .map(this::createStatsResponse)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.ok(new MonthlyStatsResponse(0.0, 0, 0.0)))
                .onErrorResume(error -> {
                    error.printStackTrace();
                    // Return a default MonthlyStatsResponse with zeros
                    MonthlyStatsResponse emptyStats = new MonthlyStatsResponse(0.0, 0, 0.0);
                    return Mono.just(ResponseEntity.ok(emptyStats));
                });
    }
    
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
     * Crée la réponse des statistiques à partir du tuple
     * 
     * @param stats tuple contenant (total heures, nombre employés)
     * @return objet de réponse des statistiques
     */
    private MonthlyStatsResponse createStatsResponse(Tuple2<Double, Integer> stats) {
        return new MonthlyStatsResponse(
                stats.getT1(), // total heures
                stats.getT2(), // nombre employés
                stats.getT2() > 0 ? stats.getT1() / stats.getT2() : 0.0 // moyenne par employé
        );
    }
    
}