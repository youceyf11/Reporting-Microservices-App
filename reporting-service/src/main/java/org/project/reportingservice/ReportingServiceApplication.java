package org.project.reportingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class ReportingServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ReportingServiceApplication.class, args);
  }
}
