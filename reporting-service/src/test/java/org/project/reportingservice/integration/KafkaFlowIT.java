package org.project.reportingservice.integration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.awaitility.Awaitility;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

import org.project.reportingservice.ReportingServiceApplication;
import org.project.reportingservice.service.ReportingService;
import org.project.issueevents.events.IssueUpsertedEvent;

@Testcontainers
@SpringBootTest(classes = ReportingServiceApplication.class)
@DirtiesContext
public class KafkaFlowIT {

  @Container
  static final KafkaContainer kafka =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.1"));

  @DynamicPropertySource
  static void kafkaProps(DynamicPropertyRegistry r) {
    r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
  }

  @Autowired
  private KafkaTemplate<String, IssueUpsertedEvent> kafkaTemplate;

  @Autowired
  private ReportingService reportingService;

  @Test
  void issueUpsertEvent_reaches_reportingService() {
    // 1) simulate jira-fetch publishing
    IssueUpsertedEvent evt = new IssueUpsertedEvent(
            "PROJ",
            "PROJ-1",
            "user@example.com",
            8.0,
            Instant.now());
    kafkaTemplate.send("jira.issue.upserted", evt.getProjectKey(), evt);

    // 2) wait & assert reporting-service produced a report
    Awaitility.await()
              .atMost(Duration.ofSeconds(10))
              .untilAsserted(() ->
                 assertThat(reportingService.generateMonthlyReport("PROJ").block())
                      .isNotNull());
  }
}