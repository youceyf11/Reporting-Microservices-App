package org.project.reportingservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO représentant le résultat complet du reporting mensuel
 * Contient les statistiques globales et le classement des employés
 */
@Getter
@Setter
public class ReportingResultDto {

    // Getters et Setters
    @JsonProperty("reportGeneratedAt")
    private LocalDateTime reportGeneratedAt;

    @JsonProperty("month")
    private String month;

    @JsonProperty("year")
    private String year;

    @JsonProperty("totalEmployees")
    private Integer totalEmployees;

    @JsonProperty("totalHoursWorked")
    private Double totalHoursWorked;

    @JsonProperty("totalIssuesResolved")
    private Integer totalIssuesResolved;

    @JsonProperty("averageResolutionTimeHours")
    private Double averageResolutionTimeHours;

    @JsonProperty("employeeRankings")
    private List<EmployeePerformanceDto> employeeRankings;

    // Constructeurs
    public ReportingResultDto() {
        this.reportGeneratedAt = LocalDateTime.now();
    }

    public ReportingResultDto(String month, String year, List<EmployeePerformanceDto> employeeRankings) {
        this();
        this.month = month;
        this.year = year;
        this.employeeRankings = employeeRankings;
        calculateTotals();
    }

    public ReportingResultDto(String month, List<EmployeePerformanceDto> employeeRankings) {
        this();
        this.month = month;
        this.employeeRankings = employeeRankings;
        calculateTotals();
    }

    /**
     * Calcule les totaux à partir des données des employés
     */
  /*  private void calculateTotals() {
        if (employeeRankings != null && !employeeRankings.isEmpty()) {
            this.totalEmployees = employeeRankings.size();
            this.totalHoursWorked = employeeRankings.stream()
                    .mapToDouble(EmployeePerformanceDto::getTotalHoursWorked)
                    .sum();
            this.totalIssuesResolved = employeeRankings.stream()
                    .mapToInt(EmployeePerformanceDto::getTotalIssuesResolved)
                    .sum();
            this.averageResolutionTimeHours = employeeRankings.stream()
                    .mapToDouble(EmployeePerformanceDto::getAverageResolutionTimeHours)
                    .average()
                    .orElse(0.0);
        }
    } */

    private void calculateTotals() {
        if (employeeRankings != null && !employeeRankings.isEmpty()) {
            this.totalEmployees = employeeRankings.size();
            this.totalHoursWorked = employeeRankings.stream()
                    .mapToDouble(e -> e.getTotalHoursWorked() != null ? e.getTotalHoursWorked() : 0.0)
                    .sum();
            this.totalIssuesResolved = employeeRankings.stream()
                    .mapToInt(e -> e.getTotalIssuesResolved() != null ? e.getTotalIssuesResolved() : 0)
                    .sum();
            this.averageResolutionTimeHours = employeeRankings.stream()
                    .mapToDouble(e -> e.getAverageResolutionTimeHours() != null ? e.getAverageResolutionTimeHours() : 0.0)
                    .average()
                    .orElse(0.0);
        }
    }



}