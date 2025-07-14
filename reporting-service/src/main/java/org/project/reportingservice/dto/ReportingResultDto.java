package org.project.reportingservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO représentant le résultat complet du reporting mensuel
 * Contient les statistiques globales et le classement des employés
 */
public class ReportingResultDto {

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
    private void calculateTotals() {
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
    }

    // Getters et Setters
    public LocalDateTime getReportGeneratedAt() {
        return reportGeneratedAt;
    }

    public void setReportGeneratedAt(LocalDateTime reportGeneratedAt) {
        this.reportGeneratedAt = reportGeneratedAt;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public Integer getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(Integer totalEmployees) {
        this.totalEmployees = totalEmployees;
    }

    public Double getTotalHoursWorked() {
        return totalHoursWorked;
    }

    public void setTotalHoursWorked(Double totalHoursWorked) {
        this.totalHoursWorked = totalHoursWorked;
    }

    public Integer getTotalIssuesResolved() {
        return totalIssuesResolved;
    }

    public void setTotalIssuesResolved(Integer totalIssuesResolved) {
        this.totalIssuesResolved = totalIssuesResolved;
    }

    public Double getAverageResolutionTimeHours() {
        return averageResolutionTimeHours;
    }

    public void setAverageResolutionTimeHours(Double averageResolutionTimeHours) {
        this.averageResolutionTimeHours = averageResolutionTimeHours;
    }

    public List<EmployeePerformanceDto> getEmployeeRankings() {
        return employeeRankings;
    }

    public void setEmployeeRankings(List<EmployeePerformanceDto> employeeRankings) {
        this.employeeRankings = employeeRankings;
        calculateTotals();
    }

    public List<EmployeePerformanceDto> getEmployeeReports() {
        return employeeRankings;
    }
}