package org.project.reportingservice.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.Year;

/**
 * Classe utilitaire pour les calculs de temps et de dates
 * Contient les méthodes pour calculer les durées, filtrer par mois, etc.
 */
public class TimeUtils {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Calcule la différence en heures entre deux dates
     * @param start Date de début
     * @param end Date de fin
     * @return Nombre d'heures entre les deux dates
     */
    public static double calculateHoursDifference(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0.0;
        }
        
        Duration duration = Duration.between(start, end);
        return duration.toMinutes() / 60.0;
    }

    /**
     * Calcule la différence en jours entre deux dates
     * @param start Date de début
     * @param end Date de fin
     * @return Nombre de jours entre les deux dates
     */
    public static long calculateDaysDifference(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        
        return ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate());
    }

    /**
     * Vérifie si une date appartient au mois courant
     * @param date Date à vérifier
     * @return true si la date est dans le mois courant
     */
    public static boolean isInCurrentMonth(LocalDateTime date) {
        if (date == null) {
            return false;
        }
        
        YearMonth currentMonth = YearMonth.now();
        YearMonth dateMonth = YearMonth.from(date);
        
        return currentMonth.equals(dateMonth);
    }

    /**
     * Vérifie si une date appartient à un mois spécifique
     * @param date Date à vérifier
     * @param targetMonth Mois cible au format "yyyy-MM"
     * @return true si la date est dans le mois spécifié
     */
    public static boolean isInMonth(LocalDateTime date, String targetMonth) {
        if (date == null || targetMonth == null) {
            return false;
        }
        
        try {
            YearMonth target = YearMonth.parse(targetMonth, MONTH_FORMATTER);
            YearMonth dateMonth = YearMonth.from(date);
            
            return target.equals(dateMonth);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Obtient le mois courant au format "yyyy-MM"
     * @return Mois courant formaté
     */
    public static String getCurrentMonth(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        return YearMonth.now().format(MONTH_FORMATTER);
    }

    /**
     * Obtient le mois d'une date au format "yyyy-MM"
     * @param date Date dont on veut extraire le mois
     * @return Mois formaté
     */
    public static String getMonthFromDate(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        
        return YearMonth.from(date).format(MONTH_FORMATTER);
    }

    /**
     * Convertit des secondes en heures
     * @param seconds Nombre de secondes
     * @return Nombre d'heures (avec décimales)
     */
    public static double secondsToHours(long seconds) {
        return seconds / 3600.0;
    }

    /**
     * Convertit des heures en secondes
     * @param hours Nombre d'heures
     * @return Nombre de secondes
     */
    public static long hoursToSeconds(double hours) {
        return Math.round(hours * 3600);
    }

    /**
     * Formate une durée en heures de manière lisible
     * @param hours Nombre d'heures
     * @return Chaîne formatée (ex: "8.5h", "0.25h")
     */
    public static String formatHours(double hours) {
        if (hours == 0) {
            return "0h";
        }
        
        if (hours < 1) {
            return String.format("%.2fh", hours);
        }
        
        if (hours == Math.floor(hours)) {
            return String.format("%.0fh", hours);
        }
        
        return String.format("%.1fh", hours);
    }

    /**
     * Calcule le pourcentage de performance par rapport à un objectif
     * @param actual Valeur actuelle
     * @param target Valeur cible
     * @return Pourcentage (0.0 à 1.0)
     */
    public static double calculatePerformancePercentage(double actual, double target) {
        if (target <= 0) {
            return 0.0;
        }
        
        return Math.min(actual / target, 1.0);
    }

    /**
     * Détermine le niveau de performance basé sur un pourcentage
     * @param percentage Pourcentage de performance (0.0 à 1.0)
     * @param excellentThreshold Seuil pour "excellent"
     * @param goodThreshold Seuil pour "bon"
     * @param poorThreshold Seuil pour "faible"
     * @return Niveau de performance
     */
    public static String determinePerformanceLevel(double percentage, 
                                                  double excellentThreshold,
                                                  double goodThreshold,
                                                  double poorThreshold) {
        if (percentage >= excellentThreshold) {
            return "EXCELLENT";
        } else if (percentage >= goodThreshold) {
            return "GOOD";
        } else if (percentage >= poorThreshold) {
            return "AVERAGE";
        } else {
            return "POOR";
        }
    }

    /**
     * Vérifie si une date est valide (non nulle et dans le passé)
     * @param date Date à vérifier
     * @return true si la date est valide
     */
    public static boolean isValidDate(LocalDateTime date) {
        return date != null && date.isBefore(LocalDateTime.now());
    }

    /**
     * Calcule le nombre de jours ouvrables dans un mois
     * @param yearMonth Mois à analyser
     * @return Nombre de jours ouvrables (approximatif)
     */
    public static Integer getWorkingDaysInMonth(YearMonth yearMonth) {
        // Approximation: 22 jours ouvrables par mois en moyenne
        return 22;
    }

    /**
     * Calcule les heures de travail attendues pour un mois donné
     * @param yearMonth Mois à analyser
     * @param hoursPerDay Heures par jour (généralement 8)
     * @return Nombre d'heures attendues
     */
    public static double getExpectedHoursForMonth(YearMonth yearMonth, double hoursPerDay) {
        return getWorkingDaysInMonth(yearMonth) * hoursPerDay;
    }

    
    public static double roundToTwoDecimals(double value) {
    return Math.round(value * 100.0) / 100.0;
    }

    public static String getCurrentYear(){  
    return String.valueOf(Year.now().getValue());
    }

}