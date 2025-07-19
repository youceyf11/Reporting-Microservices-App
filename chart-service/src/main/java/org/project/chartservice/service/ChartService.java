package org.project.chartservice.service;

import org.project.chartservice.IService.IChartService;
import org.project.chartservice.enums.ChartType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ChartService implements IChartService {
    
    private final ReportingService reportingService;
    private final ChartGenerationService chartGenerationService;
    private final EmailService emailService;
    
    @Autowired
    public ChartService(ReportingService reportingService,
                       ChartGenerationService chartGenerationService,
                       EmailService emailService) {
        this.reportingService = reportingService;
        this.chartGenerationService = chartGenerationService;
        this.emailService = emailService;
    }
    
    @Override
    public Mono<byte[]> generateMonthlyChart(String projectKey, ChartType chartType) {
        return reportingService.getMonthlyReportingData(projectKey)
                .flatMap(reportingData -> 
                    chartGenerationService.generateChart(reportingData.getEmployees(), chartType, projectKey)
                );
    }

    @Override
    public Mono<Void> generateAndEmailChart(String projectKey, ChartType chartType, String toEmail) {
        return generateMonthlyChart(projectKey, chartType)
                .flatMap(chartData -> {
                    String chartName = getChartDisplayName(chartType);
                    String subject = String.format("%s Chart - Project %s", chartName, projectKey);
                    return emailService.sendChartByEmail(toEmail, subject, chartData, chartName, projectKey);
                });
    }
    
    private String getChartDisplayName(ChartType chartType) {
        switch (chartType) {
            case WEEKLY_BAR: return "Weekly Hours";
            case MONTHLY_BAR: return "Monthly Hours";
            case COMPARATIVE: return "Comparative Analysis";
            default: return "Chart";
        }
    }
}