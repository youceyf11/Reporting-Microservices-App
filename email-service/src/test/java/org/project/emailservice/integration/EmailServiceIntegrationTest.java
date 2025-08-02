package org.project.emailservice.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.project.emailservice.config.EmailProviderConfig;
import org.project.emailservice.config.TestConfig;
import org.project.emailservice.dto.EmailRequest;
import org.project.emailservice.dto.EmailResponse;
import org.project.emailservice.entity.EmailLog;
import org.project.emailservice.enums.EmailStatus;
import org.project.emailservice.service.QueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestConfig.class)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration,org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration,org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration"
})
@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = EmailProviderConfig.class))
@DisplayName("EmailService Integration Tests - End-to-End Flow")
class EmailServiceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ReactiveRedisTemplate<String, EmailLog> redisTemplate;

    @Autowired
    private QueueService queueService;

    private ReactiveValueOperations<String, EmailLog> valueOperations;

    @BeforeEach
    void setUp() {
        this.webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();
        
        valueOperations = Mockito.mock(ReactiveValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        lenient().doNothing().when(queueService).queueEmail(any(EmailRequest.class), anyString());
    }

    @Test
    @DisplayName("POST /api/emails/send - Success")
    void testSendEmail_Success() {
        // ARRANGE
        EmailRequest request = EmailRequest.builder()
                .to("test@example.com")
                .from("noreply@example.com")
                .subject("Test Subject")
                .build();
        when(valueOperations.set(anyString(), any(EmailLog.class))).thenReturn(Mono.just(true));

        // ACT & ASSERT
        webTestClient.post().uri("/api/emails/send")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EmailResponse.class)
                .value(response -> {
                    assert response.getEmailId() != null;
                    assert response.getStatus() == EmailStatus.QUEUED;
                });
    }

    @Test
    @DisplayName("POST /api/emails/send - Validation Failure")
    void testSendEmail_ValidationFailure() {
        // ARRANGE
        EmailRequest request = EmailRequest.builder()
                .to("not-an-email")
                .subject(" ")
                .build();

        // ACT & ASSERT
        webTestClient.post().uri("/api/emails/send")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("GET /api/emails/status/{emailId} - Success")
    void testGetEmailStatus_Success() {
        // ARRANGE
        String emailId = UUID.randomUUID().toString();
        EmailLog mockEmailLog = EmailLog.builder()
                .id(emailId)
                .to("test@example.com")
                .status(EmailStatus.SENT)
                .build();
        when(valueOperations.get("email:" + emailId)).thenReturn(Mono.just(mockEmailLog));

        // ACT & ASSERT
        webTestClient.get().uri("/api/emails/status/" + emailId)
            .exchange()
            .expectStatus().isOk()
            .expectBody(EmailResponse.class)
            .value(response -> {
                assert response.getEmailId().equals(emailId);
                assert response.getStatus() == EmailStatus.SENT;
            });

        verify(valueOperations).get("email:" + emailId);
    }

    @Test
    @DisplayName("GET /api/email/status/{emailId} - Not Found")
    void testGetEmailStatus_NotFound() {
        webTestClient.get().uri("/api/email/status/non-existent-id")
                .exchange()
                .expectStatus().isNotFound();
    }
}