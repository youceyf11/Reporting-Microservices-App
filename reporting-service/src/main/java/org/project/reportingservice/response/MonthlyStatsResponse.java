package org.project.reportingservice.response;


/**
     * Classe Integererne pour la réponse des statistiques mensuelles
     */
    public class MonthlyStatsResponse {
        private double totalHoursWorked;
        private Integer totalEmployees;
        private double averageHoursPerEmployee;
        
        public MonthlyStatsResponse(double totalHoursWorked, Integer totalEmployees, double averageHoursPerEmployee) {
            this.totalHoursWorked = totalHoursWorked;
            this.totalEmployees = totalEmployees;
            this.averageHoursPerEmployee = averageHoursPerEmployee;
        }
        
        // Getters
        public double getTotalHoursWorked() { return totalHoursWorked; }
        public Integer getTotalEmployees() { return totalEmployees; }
        public double getAverageHoursPerEmployee() { return averageHoursPerEmployee; }
        
        // Setters
        public void setTotalHoursWorked(double totalHoursWorked) { this.totalHoursWorked = totalHoursWorked; }
        public void setTotalEmployees(Integer totalEmployees) { this.totalEmployees = totalEmployees; }
        public void setAverageHoursPerEmployee(double averageHoursPerEmployee) { this.averageHoursPerEmployee = averageHoursPerEmployee; }
    }