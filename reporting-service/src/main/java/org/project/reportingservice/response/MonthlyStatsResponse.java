package org.project.reportingservice.response;


import lombok.*;

/**
     * Classe Integererne pour la réponse des statistiques mensuelles
     */
   @Data
   @AllArgsConstructor
   @NoArgsConstructor
    public class MonthlyStatsResponse {
    // Setters
    // Getters
        private Double totalHoursWorked;
        private Integer totalEmployees;
        private Double averageHoursPerEmployee;
        

}