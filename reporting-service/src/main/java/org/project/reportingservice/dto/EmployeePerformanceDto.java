package org.project.reportingservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.project.reportingservice.dto.EmployeePerformanceDto;

    /**
     * DTO représentant les performances d'un employé
     */
    public class EmployeePerformanceDto {

        @JsonProperty("employeeEmail")
        private String employeeEmail;

        @JsonProperty("totalHoursWorked")
        private Double totalHoursWorked;

        @JsonProperty("expectedHoursThisMonth")
         private Double expectedHoursThisMonth; 


        @JsonProperty("totalIssuesResolved")
        private Integer totalIssuesResolved;

        @JsonProperty("averageResolutionTimeHours")
        private Double averageResolutionTimeHours;

        @JsonProperty("performancePercentage")
        private Double performancePercentage;

        @JsonProperty("performanceLevel")
        private String performanceLevel;

        @JsonProperty("ranking")
        private Integer ranking;

        // Constructeurs
        public EmployeePerformanceDto() {}

        public EmployeePerformanceDto(String employeeEmail, Double totalHoursWorked, 
                                    Integer totalIssuesResolved, Double averageResolutionTimeHours,
                                    Double performancePercentage, String performanceLevel,Double expectedHoursThisMonth) {
            this.employeeEmail = employeeEmail;
            this.totalHoursWorked = totalHoursWorked;
            this.totalIssuesResolved = totalIssuesResolved;
            this.averageResolutionTimeHours = averageResolutionTimeHours;
            this.performancePercentage = performancePercentage;
            this.performanceLevel = performanceLevel;
            this.expectedHoursThisMonth = expectedHoursThisMonth;
        }

        // Getters et Setters
        public String getEmployeeEmail() {
            return employeeEmail;
        }

        public void setEmployeeEmail(String employeeEmail) {
            this.employeeEmail = employeeEmail;
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

        public Double getExpectedHoursThisMonth() {
            return expectedHoursThisMonth;
        }
        public void setExpectedHoursThisMonth(Double expectedHoursThisMonth) {
            this.expectedHoursThisMonth = expectedHoursThisMonth;
        }
        public Double getAverageResolutionTimeHours() {
            return averageResolutionTimeHours;
        }

        public void setAverageResolutionTimeHours(Double averageResolutionTimeHours) {
            this.averageResolutionTimeHours = averageResolutionTimeHours;
        }

        public Double getPerformancePercentage() {
            return performancePercentage;
        }

        public void setPerformancePercentage(Double performancePercentage) {
            this.performancePercentage = performancePercentage;
        }

        public String getPerformanceLevel() {
            return performanceLevel;
        }

        public void setPerformanceLevel(String performanceLevel) {
            this.performanceLevel = performanceLevel;
        }

        public Integer getRanking() {
            return ranking;
        }

        public void setRanking(Integer ranking) {
            this.ranking = ranking;
        }

        @Override
        public String toString() {
            return "EmployeePerformanceDto{" +
                    "employeeEmail='" + employeeEmail + '\'' +
                    ", totalHoursWorked=" + totalHoursWorked +
                    ", totalIssuesResolved=" + totalIssuesResolved +
                    ", performancePercentage=" + performancePercentage +
                    '}';
        }
    }