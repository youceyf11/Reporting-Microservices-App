package org.project.reportingservice.iservice;

import org.project.reportingservice.dto.EmployeePerformanceDto;
import org.project.reportingservice.dto.ReportingResultDto;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;

public interface IReportingService {
    Mono<ReportingResultDto> generateMonthlyReport(String projectKey);
    Mono<List<EmployeePerformanceDto>> getTopActiveEmployees(String projectKey, Integer topN);
    Mono<Tuple2<Double, Integer>> getMonthlyStatistics(String projectKey);
}