package org.project.emailservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.project.emailservice.dto.EmailRequest;
import org.project.emailservice.dto.EmailResponse;
import org.project.emailservice.dto.EmailAttachment;
import org.project.emailservice.entity.EmailLog;
import org.project.emailservice.enums.EmailStatus;
import org.project.emailservice.enums.EmailPriority;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour EmailService
 * 
 * LOGIQUE DES TESTS:
 * 1. Test de la logique métier principale (envoi d'emails, gestion des statuts)
 * 2. Test des interactions avec Redis (cache, persistance des logs)
 * 3. Test des interactions avec les services dépendants (Queue, Template, Provider)
 * 4. Test de la gestion des erreurs et des cas limites
 * 5. Test du comportement réactif avec Project Reactor
 * 6. Test des opérations en lot et de la performance
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Tests - Tests de la logique métier principale")
class EmailServiceTest {

    @Mock
    private QueueService queueService;

    @Mock
    private TemplateService templateService;

    @Mock
    private ProviderService providerService;

    @Mock
    private ReactiveRedisTemplate<String, EmailLog> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, EmailLog> valueOperations;

    @InjectMocks
    private EmailService emailService;

    private EmailRequest validEmailRequest;
    private EmailLog mockEmailLog;
    private EmailResponse mockEmailResponse;

    @BeforeEach // @BeforeEach est un hook qui s'exécute avant chaque test
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.set(anyString(), any(EmailLog.class), any(Duration.class))).thenReturn(Mono.just(true));
        lenient().when(valueOperations.get(anyString())).thenReturn(Mono.empty());
        lenient().when(queueService.queueEmail(any(EmailRequest.class), anyString())).thenReturn(Mono.empty());
        lenient().when(templateService.renderTemplate(anyString(), anyMap())).thenReturn(Mono.just("<html>Test</html>"));
        lenient().when(providerService.sendEmail(any(EmailRequest.class))).thenReturn(Mono.just("test-message-id"));

        validEmailRequest = EmailRequest.builder()
                .to("test@example.com")
                .from("sender@example.com")
                .subject("Test Email")
                .templateName("test-template")
                .templateData(Map.of("name", "Test User", "message", "Hello World"))
                .priority(EmailPriority.NORMAL)
                .attachments(List.of(
                    EmailAttachment.builder()
                        .filename("test.pdf")
                        .content("test-content".getBytes())
                        .contentType("application/pdf")
                        .build()
                ))
                .build();

        mockEmailLog = EmailLog.builder()
                .id("test-email-id")
                .to("test@example.com")
                .from("sender@example.com")
                .subject("Test Email")
                .status(EmailStatus.QUEUED)
                .createdAt(Instant.now())
                .build();

        mockEmailResponse = EmailResponse.builder()
                .emailId("test-email-id")
                .status(EmailStatus.QUEUED)
                .message("Email queued successfully")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("sendEmail() - Envoi d'email réussi avec mise en queue")
    void testSendEmail_Success() {
        // ACT
        Mono<EmailResponse> result = emailService.processEmailRequest(validEmailRequest);

        // ASSERT
        StepVerifier.create(result)
                .expectNextMatches(response -> 
                    response.getEmailId() != null &&
                    response.getStatus() == EmailStatus.QUEUED
                )
                .verifyComplete();

        verify(valueOperations).set(anyString(), any(EmailLog.class), any(Duration.class));
        verify(queueService).queueEmail(any(EmailRequest.class));
    }

    @Test
    @DisplayName("sendEmail() - Échec de sauvegarde Redis")
    void testSendEmail_RedisFailure() {
        // ARRANGE
        lenient().when(valueOperations.set(anyString(), any(EmailLog.class), any(Duration.class)))
                .thenReturn(Mono.error(new RuntimeException("Redis connection failed")));

        // ACT
        Mono<EmailResponse> result = emailService.processEmailRequest(validEmailRequest);

        // ASSERT
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        // Vérifier que la queue n'est pas appelée en cas d'erreur Redis
        verify(queueService, never()).queueEmail(any(EmailRequest.class));
    }

    @Test
    @DisplayName("sendEmail() - Échec de mise en queue")
    void testSendEmail_QueueFailure() {
        // ARRANGE
        lenient().when(valueOperations.set(anyString(), any(EmailLog.class), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(queueService.queueEmail(any(EmailRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("RabbitMQ connection failed")));

        // ACT
        Mono<EmailResponse> result = emailService.processEmailRequest(validEmailRequest);

        // ASSERT
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(valueOperations).set(anyString(), any(EmailLog.class), any(Duration.class));
        verify(queueService).queueEmail(any(EmailRequest.class));
    }

    @Test
    @DisplayName("sendChartEmail - Succès de l'envoi")
    void testSendChartEmail_Success() {
        // ARRANGE
        byte[] chartData = "fake-chart-data".getBytes();
        EmailRequest request = EmailRequest.builder()
                .to("recipient@example.com")
                .subject("Monthly Report")
                .body("Please find the attached chart.")
                .attachments(List.of(new EmailAttachment("chart.png", chartData, "image/png")))
                .priority(EmailPriority.HIGH)
                .build();

        when(templateService.generateChartEmailContent(anyMap())).thenReturn(Mono.just("<html><body>Generated Chart</body></html>"));
        when(queueService.sendMessage(any(EmailRequest.class), eq(EmailPriority.HIGH))).thenReturn(Mono.empty());

        // ACT
        StepVerifier.create(emailService.sendChartEmail(chartData, request.getTo(), request.getSubject()))
                // ASSERT
                .expectNextMatches(response -> {
                    assert response.getEmailId() != null;
                    assert response.getStatus() == EmailStatus.QUEUED;
                    assert response.getMessage().contains("queued");
                    verify(queueService).sendMessage(any(EmailRequest.class), eq(EmailPriority.HIGH));
                    verify(templateService).generateChartEmailContent(anyMap());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getEmailStatus - Statut trouvé dans Redis")
    void testGetEmailStatus_Found() {
        // ARRANGE
        String emailId = "found-id";
        EmailLog foundLog = EmailLog.builder()
                .id(emailId)
                .to("to@example.com")
                .from("from@example.com")
                .subject("Test Subject")
                .status(EmailStatus.SENT)
                .priority(EmailPriority.NORMAL)
                .createdAt(Instant.now())
                .build();

        when(valueOperations.get(emailId)).thenReturn(Mono.just(foundLog));

        // ACT
        StepVerifier.create(emailService.getEmailStatus(emailId))
                // ASSERT
                .expectNextMatches(response -> 
                     response.getEmailId().equals(emailId) &&
                     response.getStatus() == EmailStatus.SENT
                )
                .verifyComplete();

        verify(valueOperations).get(emailId);
    }

    @Test
    @DisplayName("getEmailStatus - Statut non trouvé dans Redis")
    void testGetEmailStatus_NotFound() {
        // ARRANGE
        String emailId = "not-found-id";
        when(valueOperations.get(emailId)).thenReturn(Mono.empty());

        // ACT
        StepVerifier.create(emailService.getEmailStatus(emailId))
                // ASSERT
                .expectNextMatches(response -> 
                        response.getStatus() == EmailStatus.NOT_FOUND &&
                        response.getEmailId().equals(emailId)
                )
                .verifyComplete();

        verify(valueOperations).get(emailId);
    }

    @Test
    @DisplayName("getBulkEmailStatus - Succès avec tous les statuts trouvés")
    void testGetBulkEmailStatus_AllFound() {
        // ARRANGE
        List<String> emailIds = List.of("id1", "id2");
        EmailLog log1 = EmailLog.builder()
                .id("id1")
                .to("to1@example.com")
                .from("from1@example.com")
                .subject("Test Subject 1")
                .status(EmailStatus.SENT)
                .priority(EmailPriority.NORMAL)
                .createdAt(Instant.now())
                .build();
        EmailLog log2 = EmailLog.builder()
                .id("id2")
                .to("to2@example.com")
                .from("from2@example.com")
                .subject("Test Subject 2")
                .status(EmailStatus.FAILED)
                .priority(EmailPriority.HIGH)
                .createdAt(Instant.now())
                .build();

        when(valueOperations.get("id1")).thenReturn(Mono.just(log1));
        when(valueOperations.get("id2")).thenReturn(Mono.just(log2));

        // ACT
        StepVerifier.create(emailService.getBulkEmailStatus(emailIds))
                // ASSERT
                .expectNextMatches(response -> response.getEmailId().equals("id1") && response.getStatus() == EmailStatus.SENT)
                .expectNextMatches(response -> response.getEmailId().equals("id2") && response.getStatus() == EmailStatus.FAILED)
                .verifyComplete();

        verify(valueOperations).get("id1");
        verify(valueOperations).get("id2");
    }

    @Test
    @DisplayName("getBulkEmailStatus - Certains statuts non trouvés")
    void testGetBulkEmailStatus_SomeNotFound() {
        // ARRANGE
        List<String> emailIds = List.of("id1", "id-not-found");
        EmailLog log1 = EmailLog.builder()
                .id("id1")
                .to("to1@example.com")
                .from("from1@example.com")
                .subject("Test Subject 1")
                .status(EmailStatus.SENT)
                .priority(EmailPriority.NORMAL)
                .createdAt(Instant.now())
                .build();

        when(valueOperations.get("id1")).thenReturn(Mono.just(log1));
        when(valueOperations.get("id-not-found")).thenReturn(Mono.empty());

        // ACT
        StepVerifier.create(emailService.getBulkEmailStatus(emailIds))
                // ASSERT
                .expectNextMatches(response -> response.getEmailId().equals("id1") && response.getStatus() == EmailStatus.SENT)
                .expectNextMatches(response -> response.getEmailId().equals("id-not-found") && response.getStatus() == EmailStatus.NOT_FOUND)
                .verifyComplete();

        verify(valueOperations).get("id1");
        verify(valueOperations).get("id-not-found");
    }

    @Test
    @DisplayName("updateEmailStatus - Mise à jour réussie")
    void testUpdateEmailStatus_Success() {
        // ARRANGE
        String emailId = "update-id";
        EmailLog existingLog = EmailLog.builder()
                .id(emailId)
                .to("to@example.com")
                .from("from@example.com")
                .subject("Test Subject")
                .status(EmailStatus.QUEUED)
                .priority(EmailPriority.NORMAL)
                .createdAt(Instant.now())
                .build();

        when(valueOperations.get(emailId)).thenReturn(Mono.just(existingLog));
        when(valueOperations.set(eq(emailId), any(EmailLog.class))).thenReturn(Mono.just(true));

        // ACT
        StepVerifier.create(emailService.updateEmailStatus(emailId, EmailStatus.SENT, "provider-name", null))
                // ASSERT
                .verifyComplete();

        verify(valueOperations).get(emailId);
        verify(valueOperations).set(eq(emailId), any(EmailLog.class));
    }

    @Test
    @DisplayName("updateEmailStatus - Log initial non trouvé")
    void testUpdateEmailStatus_LogNotFound() {
        // ARRANGE
        String emailId = "not-found-id";
        when(valueOperations.get(emailId)).thenReturn(Mono.empty());

        // ACT
        StepVerifier.create(emailService.updateEmailStatus(emailId, EmailStatus.SENT, "provider-name", null))
                // ASSERT
                .verifyComplete();

        verify(valueOperations).get(emailId);
    }

    @Test
    @DisplayName("updateEmailStatus - Échec de la mise à jour Redis")
    void testUpdateEmailStatus_RedisSetFailed() {
        // ARRANGE
        String emailId = "update-fail-id";
        EmailLog existingLog = EmailLog.builder()
                .id(emailId)
                .to("to@example.com")
                .from("from@example.com")
                .subject("Test Subject")
                .status(EmailStatus.QUEUED)
                .priority(EmailPriority.NORMAL)
                .createdAt(Instant.now())
                .build();

        when(valueOperations.get(emailId)).thenReturn(Mono.just(existingLog));
        when(valueOperations.set(eq(emailId), any(EmailLog.class))).thenReturn(Mono.just(false));

        // ACT
        StepVerifier.create(emailService.updateEmailStatus(emailId, EmailStatus.SENT, "provider-name", null))
                // ASSERT
                .verifyComplete();

        verify(valueOperations).get(emailId);
        verify(valueOperations).set(eq(emailId), any(EmailLog.class));
    }

    @Test
    @DisplayName("Test de performance - Envoi de multiples emails")
    void testSendMultipleEmails_Performance() {
        // ARRANGE
        lenient().when(valueOperations.set(anyString(), any(EmailLog.class), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(queueService.queueEmail(any(EmailRequest.class)))
                .thenReturn(Mono.empty());

        // ACT - Envoyer 10 emails en parallèle
        Flux<EmailResponse> result = Flux.range(1, 10)
                .flatMap(i -> {
                    EmailRequest request = validEmailRequest.toBuilder()
                            .to("test" + i + "@example.com")
                            .build();
                    return emailService.processEmailRequest(request);
                });

        // ASSERT
        StepVerifier.create(result)
                .expectNextCount(10)
                .verifyComplete();

        // Vérifier que tous les emails ont été traités
        verify(valueOperations, times(10)).set(anyString(), any(EmailLog.class), any(Duration.class));
        verify(queueService, times(10)).queueEmail(any(EmailRequest.class));
    }

    @Test
    @DisplayName("Test de validation des données d'entrée")
    void testEmailValidation() {
        // ARRANGE - Email avec données manquantes
        EmailRequest invalidRequest = EmailRequest.builder()
                .subject("Test")
                // Pas d'adresse destinataire
                .build();

        lenient().when(valueOperations.set(anyString(), any(EmailLog.class), any(Duration.class)))
                .thenReturn(Mono.just(true));

        // ACT & ASSERT - Le service doit gérer les données invalides
        StepVerifier.create(emailService.processEmailRequest(invalidRequest))
                .expectNextMatches(response -> response.getEmailId() != null)
                .verifyComplete();
    }

    @Test
    @DisplayName("Test de timeout et retry")
    void testEmailTimeout() {
        // ARRANGE - Simulation d'un timeout Redis
        lenient().when(valueOperations.set(anyString(), any(EmailLog.class), any(Duration.class)))
                .thenReturn(Mono.just(true).delayElement(Duration.ofSeconds(5)));

        // ACT
        Mono<EmailResponse> result = emailService.processEmailRequest(validEmailRequest)
                .timeout(Duration.ofSeconds(2));

        // ASSERT
        StepVerifier.create(result)
                .expectError()
                .verify();
    }
}
