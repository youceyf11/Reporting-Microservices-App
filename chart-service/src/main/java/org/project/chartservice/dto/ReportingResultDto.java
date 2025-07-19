package org.project.chartservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReportingResultDto {
    @JsonProperty("employees")
    private List<EmployeePerformanceDto> employees;
    
    public ReportingResultDto() {}
    
    public ReportingResultDto(List<EmployeePerformanceDto> employees) {
        this.employees = employees;
    }
    
    public List<EmployeePerformanceDto> getEmployees() { return employees; }
    public void setEmployees(List<EmployeePerformanceDto> employees) { this.employees = employees; }
}
