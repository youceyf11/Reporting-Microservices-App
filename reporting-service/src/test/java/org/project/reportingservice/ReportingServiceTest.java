package org.project.reportingservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.project.reportingservice.controller.ReportingController;
import org.project.reportingservice.dto.EmployeePerformanceDto;
import org.project.reportingservice.dto.MonthlyStatsDto;
import org.project.reportingservice.dto.ReportingResultDto;
import org.project.reportingservice.dto.WeeklyStatsDto;
import org.project.reportingservice.response.HealthResponse;
import org.project.reportingservice.response.MonthlyStatsResponse;
import org.project.reportingservice.service.ReportingService;

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
                    System.out.println("Total Employees: " + result.getTotalEmployees());
                    System.out.println("Total Hours Worked: " + result.getTotalHoursWorked());
                    System.out.println("Total Issues Resolved: " + result.getTotalIssuesResolved());
                    System.out.println("Average Resolution Time: " + result.getAverageResolutionTimeHours());
                    System.out.println("Employee Rankings: " + result.getEmployeeRankings());
                    System.out.println("Report Generated At: " + result.getReportGeneratedAt());

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
                    System.out.println("Nombre d'employés retournés: " + list.size());
                    EmployeePerformanceDto emp = list.get(0);
                    System.out.println("Email: " + emp.getEmployeeEmail());
                    System.out.println("Total Hours Worked: " + emp.getTotalHoursWorked());
                    System.out.println("Total Issues Resolved: " + emp.getTotalIssuesResolved());
                    System.out.println("Average Resolution Time: " + emp.getAverageResolutionTimeHours());
                    System.out.println("Performance Percentage: " + emp.getPerformancePercentage());
                    System.out.println("Performance Level: " + emp.getPerformanceLevel());
                    System.out.println("Expected Hours This Month: " + emp.getExpectedHoursThisMonth());
                    assertEquals("Youssef", emp.getEmployeeEmail());
                    assertEquals(10.0, emp.getTotalHoursWorked());
                    assertEquals(3, emp.getTotalIssuesResolved());
                    assertEquals(3.3, emp.getAverageResolutionTimeHours());
                    assertEquals(1.0, emp.getPerformancePercentage());
                    assertEquals("EXCELLENT", emp.getPerformanceLevel());
                    assertEquals(160.0, emp.getExpectedHoursThisMonth());
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
                    System.out.println("Assignee: " + dto.getAssignee());
                    System.out.println("Hours by week: " + dto.getHoursByWeek());
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

        System.out.println("Assignee: " + dto.getAssignee());
        System.out.println("Hours by month: " + dto.getHoursByMonth());
        System.out.println("Total monthly hours: " + dto.getTotalMonthlyHours());
        System.out.println("Expected hours: " + dto.getExpectedHours());
        System.out.println("Performance ratio: " + dto.getPerformanceRatio());
        
        assertEquals(assignee, dto.getAssignee());
        assertEquals(hoursByMonth, dto.getHoursByMonth());
        assertEquals(36.0, dto.getTotalMonthlyHours());
        assertEquals(expectedHours, dto.getExpectedHours());
        assertEquals(36.0 / 160.0, dto.getPerformanceRatio());
    }


}
