package org.project.emailservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import org.project.emailservice.service.EmailService;
import org.project.emailservice.dto.EmailResponse;
import org.project.emailservice.enums.EmailStatus;

import org.project.emailservice.entity.EmailLog;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

/**
 * Tests unitaires pour EmailStatusController
 * 
 * LOGIQUE DES TESTS:
 * 1. Test des endpoints de statut dédiés (/api/email/status)
 * 2. Vérification de la récupération de statuts individuels et en lot
 * 3. Test de la gestion des cas d'erreur et des emails non trouvés
 * 4. Validation du comportement réactif avec Flux et Mono
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailStatusController Tests - Tests des endpoints de statut")
class EmailStatusControllerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailStatusController emailStatusController;

    private WebTestClient webTestClient;
    private EmailLog mockEmailLog;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(emailStatusController).build();
        
        mockEmailLog = new EmailLog("test-email-id", "test@example.com", EmailStatus.SENT);
    }

    @Test
    @DisplayName("GET /api/email/status/{emailId} - Récupération de statut réussie")
    void testGetEmailStatus_Success() {
        // ARRANGE
        String emailId = "test-email-id";
        EmailResponse mockResponse = EmailResponse.builder()
                .emailId(emailId)
                .status(EmailStatus.QUEUED)
                .message("Email is queued for sending.")
                .priority("NORMAL")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(emailService.getEmailStatus(emailId))
                .thenReturn(Mono.just(mockResponse));

        // ACT & ASSERT
        webTestClient.get()
                .uri("/api/email/status/test-email-id")
                .exchange()
                .expectStatus().isOk()
                .expectBody(EmailLog.class)
                .value(response -> {
                    assert response.getId().equals("test-email-id");
                    assert response.getStatus() == EmailStatus.SENT;
                });

        verify(emailService).getEmailStatus("test-email-id");
    }

    @Test
    @DisplayName("GET /api/email/status/{emailId} - Email non trouvé")
    void testGetEmailStatus_NotFound() {
        // ARRANGE
        when(emailService.getEmailStatus(anyString())).thenReturn(Mono.empty());

        // ACT & ASSERT
        webTestClient.get()
                .uri("/api/email/status/non-existent-id")
                .exchange()
                .expectStatus().isNotFound();

        //verify(emailService).getEmailStatus("non-existent-id");
    }

    @Test
    @DisplayName("POST /api/email/status/bulk - Récupération de statuts multiples")
    void testGetBulkEmailStatus_Success() {
        // ARRANGE
        List<String> emailIds = List.of("id1", "id2", "id3");
        EmailResponse response1 = EmailResponse.builder().emailId("id1").status(EmailStatus.SENT).build();
        EmailResponse response2 = EmailResponse.builder().emailId("id2").status(EmailStatus.FAILED).build();
        EmailResponse response3 = EmailResponse.builder().emailId("id3").status(EmailStatus.QUEUED).build();

        when(emailService.getBulkEmailStatus(emailIds))
                .thenReturn(Flux.just(response1, response2, response3));

        // ACT & ASSERT
        webTestClient.post()
                .uri("/api/email/status/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(emailIds)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EmailResponse.class)
                .hasSize(3)
                .value(responses -> {
                    assert responses.get(0).getStatus() == EmailStatus.SENT;
                    assert responses.get(1).getStatus() == EmailStatus.FAILED;
                    assert responses.get(2).getStatus() == EmailStatus.QUEUED;
                });

        verify(emailService).getBulkEmailStatus(emailIds);
    }

    @Test
    @DisplayName("POST /api/email/status/bulk - Liste vide")
    void testGetBulkEmailStatus_EmptyList() {
        // ARRANGE
        List<String> emptyList = List.of();
        when(emailService.getBulkEmailStatus(emptyList))
                .thenReturn(Flux.empty());

        // ACT & ASSERT
        webTestClient.post()
                .uri("/api/email/status/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(emptyList)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EmailLog.class)
                .hasSize(0);

        verify(emailService).getBulkEmailStatus(emptyList);
    }

    @Test
    @DisplayName("GET /api/email/status/{emailId} - Erreur du service")
    void testGetEmailStatus_ServiceError() {
        // ARRANGE
        when(emailService.getEmailStatus("error-id"))
                .thenReturn(Mono.error(new RuntimeException("Redis connection failed")));

        // ACT & ASSERT
        webTestClient.get()
                .uri("/api/email/status/error-id")
                .exchange()
                .expectStatus().is5xxServerError();

        verify(emailService).getEmailStatus("error-id");
    }
}
