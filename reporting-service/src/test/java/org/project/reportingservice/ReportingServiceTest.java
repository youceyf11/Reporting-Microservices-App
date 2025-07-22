package org.project.reportingservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.project.reportingservice.controller.ReportingController;
import org.project.reportingservice.dto.*;
import org.project.reportingservice.response.HealthResponse;
import org.project.reportingservice.response.MonthlyStatsResponse;
import org.project.reportingservice.service.ReportingService;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ReportingServiceTest {

    @Mock
    private ReportingService reportingService;

    @InjectMocks
    private ReportingController controller;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        webTestClient = WebTestClient.bindToController(controller).build();
    }


    // reporting-service/src/test/java/org/project/reportingservice/ReportingServiceTest.java

    @Test
    void testGetMonthlyReport() {
        List<EmployeePerformanceDto> employees = List.of(
                new EmployeePerformanceDto("Youssef", 20.0, 3, 2.0, 1.0, "EXCELLENT", 160.0),
                new EmployeePerformanceDto("Amine", 20.0, 2, 3.0, 0.8, "BON", 160.0)
        );
        ReportingResultDto mockResult = new ReportingResultDto("2025-07", "2025", employees);

        when(reportingService.generateMonthlyReport("PROJ"))
                .thenReturn(Mono.just(mockResult));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/reporting/monthly")
                        .queryParam("projectKey", "PROJ").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(ReportingResultDto.class)
                .value(result -> {
                    System.out.println("===== Rapport Mensuel Généré =====");
                    System.out.println("Mois: " + result.getMonth());
                    System.out.println("Année: " + result.getYear());
                    System.out.println("Nombre total d'employés: " + result.getTotalEmployees());
                    System.out.println("Heures totales travaillées: " + result.getTotalHoursWorked());
                    System.out.println("Tickets résolus: " + result.getTotalIssuesResolved());
                    System.out.println("Temps moyen de résolution: " + result.getAverageResolutionTimeHours());
                    System.out.println("Classement des employés:");
                    for (EmployeePerformanceDto emp : result.getEmployeeRankings()) {
                        System.out.println("  - " + emp.getEmployeeEmail() +
                                " | Heures: " + emp.getTotalHoursWorked() +
                                " | Tickets: " + emp.getTotalIssuesResolved() +
                                " | Performance: " + emp.getPerformanceLevel());
                    }
                    System.out.println("Date de génération du rapport: " + result.getReportGeneratedAt());
                    System.out.println("==================================");

                    assertEquals("2025-07", result.getMonth());
                    assertEquals("2025", result.getYear());
                    assertNotNull(result.getEmployeeRankings());
                    assertEquals(2, result.getTotalEmployees());
                    assertEquals(40.0, result.getTotalHoursWorked());
                    assertEquals(5, result.getTotalIssuesResolved());
                    assertEquals(2.5, result.getAverageResolutionTimeHours());
                    assertNotNull(result.getReportGeneratedAt());
                });
    }

    @Test
    void testGetTopActiveEmployees() {
        List<EmployeePerformanceDto> topEmployees = List.of(
                new EmployeePerformanceDto("Youssef", 10.0, 3, 3.3, 1.0, "EXCELLENT", 160.0)
        );

        when(reportingService.getTopActiveEmployees("PROJ", 1))
                .thenReturn(Mono.just(topEmployees));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/reporting/monthly/top")
                        .queryParam("projectKey", "PROJ")
                        .queryParam("limit", "1").build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EmployeePerformanceDto.class)
                .value(list -> {
                    System.out.println("===== Top Employés Actifs =====");
                    System.out.println("Nombre d'employés retournés: " + list.size());
                    for (EmployeePerformanceDto emp : list) {
                        System.out.println("Email: " + emp.getEmployeeEmail());
                        System.out.println("Total Hours Worked: " + emp.getTotalHoursWorked());
                        System.out.println("Total Issues Resolved: " + emp.getTotalIssuesResolved());
                        System.out.println("Average Resolution Time: " + emp.getAverageResolutionTimeHours());
                        System.out.println("Performance Percentage: " + emp.getPerformancePercentage());
                        System.out.println("Performance Level: " + emp.getPerformanceLevel());
                        System.out.println("Expected Hours This Month: " + emp.getExpectedHoursThisMonth());
                    }
                    System.out.println("===============================");

                    assertEquals("Youssef", list.get(0).getEmployeeEmail());
                    assertEquals(10.0, list.get(0).getTotalHoursWorked());
                    assertEquals(3, list.get(0).getTotalIssuesResolved());
                    assertEquals(3.3, list.get(0).getAverageResolutionTimeHours());
                    assertEquals(1.0, list.get(0).getPerformancePercentage());
                    assertEquals("EXCELLENT", list.get(0).getPerformanceLevel());
                    assertEquals(160.0, list.get(0).getExpectedHoursThisMonth());
                });
    }

    @Test
    void testGetMonthlyStatistics() {
        when(reportingService.getMonthlyStatistics("PROJ"))
                .thenReturn(Mono.just(Tuples.of(40.0, 2)));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/reporting/monthly/stats")
                        .queryParam("projectKey", "PROJ").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(MonthlyStatsResponse.class)
                .value(response -> {
                    System.out.println("===== Statistiques Mensuelles =====");
                    System.out.println("Heures totales travaillées: " + response.getTotalHoursWorked());
                    System.out.println("Nombre total d'employés: " + response.getTotalEmployees());
                    System.out.println("Moyenne d'heures par employé: " + response.getAverageHoursPerEmployee());
                    System.out.println("===================================");

                    assertEquals(40.0, response.getTotalHoursWorked());
                    assertEquals(2, response.getTotalEmployees());
                    assertEquals(20.0, response.getAverageHoursPerEmployee());
                });
    }

    @Test
    void testHealthCheck() {
        webTestClient.get()
                .uri("/api/reporting/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody(HealthResponse.class)
                .value(response -> {
                    System.out.println("===== Health Check =====");
                    System.out.println("Status: " + response.getStatus());
                    System.out.println("Message: " + response.getMessage());
                    System.out.println("========================");

                    assertEquals("UP", response.getStatus());
                    assertEquals("Reporting service is healthy", response.getMessage());
                });
    }

    @Test
    void testGetWeeklyStatistics() {
        Map<Integer, Double> youssefWeeks = new HashMap<>();
        youssefWeeks.put(27, 8.0);
        Map<String, Map<Integer, Double>> weeklyStats = new HashMap<>();
        weeklyStats.put("Youssef", youssefWeeks);

        when(reportingService.getEmployeeWeeklyStats("PROJ", "Youssef"))
                .thenReturn(Mono.just(new WeeklyStatsDto("Youssef", youssefWeeks)));

        webTestClient.get()
                .uri("/api/reporting/weekly/stats/PROJ/Youssef")
                .exchange()
                .expectStatus().isOk()
                .expectBody(WeeklyStatsDto.class)
                .value(dto -> {
                    System.out.println("===== Statistiques Hebdomadaires Employé =====");
                    System.out.println("Assignee: " + dto.getAssignee());
                    System.out.println("Hours by week: " + dto.getHoursByWeek());
                    System.out.println("==============================================");

                    assertEquals("Youssef", dto.getAssignee());
                    assertEquals(youssefWeeks, dto.getHoursByWeek());
                });
    }

    @Test
    void testMonthlyStatsDto() {
        String assignee = "Amine";
        Map<Integer, Double> hoursByMonth = Map.of(7, 16.0, 8, 20.0);
        Double expectedHours = 160.0;

        MonthlyStatsDto dto = new MonthlyStatsDto(assignee, hoursByMonth, expectedHours);

        System.out.println("===== DTO Statistiques Mensuelles Employé =====");
        System.out.println("Assignee: " + dto.getAssignee());
        System.out.println("Hours by month: " + dto.getHoursByMonth());
        System.out.println("Total monthly hours: " + dto.getTotalMonthlyHours());
        System.out.println("Expected hours: " + dto.getExpectedHours());
        System.out.println("Performance ratio: " + dto.getPerformanceRatio());
        System.out.println("===============================================");

        assertEquals(assignee, dto.getAssignee());
        assertEquals(hoursByMonth, dto.getHoursByMonth());
        assertEquals(36.0, dto.getTotalMonthlyHours());
        assertEquals(expectedHours, dto.getExpectedHours());
        assertEquals(36.0 / 160.0, dto.getPerformanceRatio());
    }

    @Test
    void testGetDetailedMonthlyStatistics() {
        Map<Integer, Double> amineMonths = Map.of(7, 16.0, 8, 20.0);
        Map<String, Map<Integer, Double>> monthlyStats = Map.of("Amine", amineMonths);

        when(reportingService.getDetailedMonthlyStatistics("PROJ"))
                .thenReturn(Mono.just(monthlyStats));

        System.out.println("\n===== [TEST] DÉTAILS STATISTIQUES MENSUELLES PAR EMPLOYÉ =====");
        System.out.println("Projet: PROJ");
        System.out.println("Statistiques attendues:");
        monthlyStats.forEach((employee, months) -> {
            System.out.println("  - Employé: " + employee);
            months.forEach((month, hours) -> {
                System.out.println("      Mois: " + month + " => Heures: " + hours);
            });
        });
        System.out.println("==============================================================");

        webTestClient.get()
                .uri("/api/reporting/monthly/detailed/PROJ")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Map<Integer, Double>>>() {})
                .value(map -> {
                    System.out.println("\n>>> [RÉSULTATS ACTUELS]");
                    map.forEach((employee, stats) -> {
                        System.out.println("Employé: " + employee + ", Détails: " + stats);
                    });
                    System.out.println("--------------------------------------------------------------");

                    assertTrue(map.containsKey("Amine"));
                    assertEquals(amineMonths, map.get("Amine"));
                });
    }

    @Test
    void testGetEmployeeWeeklyStats() {
        Map<Integer, Double> weeks = Map.of(27, 8.0, 28, 12.0);
        WeeklyStatsDto dto = new WeeklyStatsDto("Youssef", weeks);

        when(reportingService.getEmployeeWeeklyStats("PROJ", "Youssef"))
                .thenReturn(Mono.just(dto));

        webTestClient.get()
                .uri("/api/reporting/weekly/stats/PROJ/Youssef")
                .exchange()
                .expectStatus().isOk()
                .expectBody(WeeklyStatsDto.class)
                .value(result -> {
                    System.out.println("===== Statistiques Hebdomadaires Employé =====");
                    System.out.println("Assignee: " + result.getAssignee());
                    System.out.println("Hours by week: " + result.getHoursByWeek());
                    System.out.println("==============================================");

                    assertEquals("Youssef", result.getAssignee());
                    assertEquals(weeks, result.getHoursByWeek());
                });
    }

    @Test
    void testGetEmployeeMonthlyStats() {
        Map<Integer, Double> months = Map.of(7, 16.0, 8, 20.0);
        Double expectedHours = 160.0;
        MonthlyStatsDto dto = new MonthlyStatsDto("Amine", months, expectedHours);

        when(reportingService.getEmployeeMonthlyStats("PROJ", "Amine", expectedHours))
                .thenReturn(Mono.just(dto));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/reporting/monthly/stats/PROJ/Amine")
                        .queryParam("expectedHours", expectedHours).build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(MonthlyStatsDto.class)
                .value(result -> {
                    System.out.println("===== Statistiques Mensuelles Employé =====");
                    System.out.println("Assignee: " + result.getAssignee());
                    System.out.println("Hours by month: " + result.getHoursByMonth());
                    System.out.println("Expected hours: " + result.getExpectedHours());
                    System.out.println("===========================================");

                    assertEquals("Amine", result.getAssignee());
                    assertEquals(months, result.getHoursByMonth());
                    assertEquals(expectedHours, result.getExpectedHours());
                });
    }
}
