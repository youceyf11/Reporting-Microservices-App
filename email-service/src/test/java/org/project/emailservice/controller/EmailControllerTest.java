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
import reactor.test.StepVerifier;

import org.project.emailservice.service.EmailService;
import org.project.emailservice.dto.EmailRequest;
import org.project.emailservice.dto.EmailResponse;
import org.project.emailservice.enums.EmailStatus;
import org.project.emailservice.enums.EmailPriority;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/**
 * Tests unitaires pour EmailController
 * 
 * LOGIQUE DES TESTS:
 * 1. Test des endpoints REST avec différents scénarios (succès, erreur, validation)
 * 2. Vérification des réponses HTTP et du contenu JSON
 * 3. Test de l'intégration avec EmailService (mocking)
 * 4. Validation des paramètres d'entrée et gestion d'erreurs
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailController Tests - Tests des endpoints REST")
class EmailControllerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailController emailController;

    private WebTestClient webTestClient;
    private EmailRequest validEmailRequest;
    private EmailResponse mockEmailResponse;

    @BeforeEach
    void setUp() {
        // Configuration du WebTestClient pour tester les endpoints
        webTestClient = WebTestClient.bindToController(emailController).build();
        
        // Données de test réutilisables
        validEmailRequest = EmailRequest.builder()
                .to("test@example.com")
                .from("sender@example.com")
                .subject("Test Email")
                .templateName("test-template")
                .templateData(Map.of("name", "Test User"))
                .priority(EmailPriority.NORMAL)
                .build();

        mockEmailResponse = EmailResponse.builder()
                .emailId("test-email-id")
                .status(EmailStatus.QUEUED)
                .message("Email queued successfully")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/emails/send - Envoi d'email réussi")
    void testSendEmail_Success() {
        // ARRANGE: Préparer le mock du service
        when(emailService.processEmailRequest(any(EmailRequest.class)))
                .thenReturn(Mono.just(mockEmailResponse));

        // ACT & ASSERT: Tester l'endpoint
        webTestClient.post()
                .uri("/api/emails/send")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validEmailRequest)
                .exchange()
                .expectStatus().isAccepted() // 202 ACCEPTED for async operations
                .expectBody(EmailResponse.class)
                .value(response -> {
                    assert response.getEmailId().equals("test-email-id");
                    assert response.getStatus() == EmailStatus.QUEUED;
                    assert response.getMessage().equals("Email queued successfully");
                });

        // Vérifier que le service a été appelé
        verify(emailService).processEmailRequest(any(EmailRequest.class));
    }

    @Test
    @DisplayName("POST /api/emails/send - Validation échoue avec données invalides")
    void testSendEmail_ValidationFailure() {
        // ARRANGE: Email invalide (pas d'adresse destinataire)
        EmailRequest invalidRequest = EmailRequest.builder()
                .subject("Test")
                .build();

        // ACT & ASSERT: Tester la validation
        webTestClient.post()
                .uri("/api/emails/send")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /api/emails/send - Erreur interne du service")
    void testSendEmail_ServiceError() {
        // ARRANGE: Service retourne une erreur
        when(emailService.processEmailRequest(any(EmailRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        // ACT & ASSERT: Tester la gestion d'erreur
        webTestClient.post()
                .uri("/api/emails/send")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validEmailRequest)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("POST /api/emails/send/chart - Envoi d'email avec graphique")
    void testSendChartEmail_Success() {
        // ARRANGE: Données de graphique
        byte[] chartData = "fake-chart-data".getBytes();
        when(emailService.processEmailRequest(any(EmailRequest.class)))
                .thenReturn(Mono.just(mockEmailResponse));

        // ACT & ASSERT: Tester l'endpoint de graphique
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/emails/send/chart")
                        .queryParam("to", "test@example.com")
                        .queryParam("projectKey", "PROJ-123")
                        .queryParam("chartType", "monthly")
                        .build())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .bodyValue(chartData)
                .exchange()
                .expectStatus().isAccepted() // 202 ACCEPTED for async chart email
                .expectBody(EmailResponse.class);

        verify(emailService).processEmailRequest(any(EmailRequest.class));
    }

    @Test
    @DisplayName("GET /api/emails/status/{emailId} - Récupération du statut d'un email")
    void testGetEmailStatus_Success() {
        // ARRANGE: Mock du service
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

        // ACT & ASSERT: Tester la récupération de statut
        webTestClient.get()
                .uri("/api/emails/status/" + emailId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EmailResponse.class)
                .value(response -> {
                    assert response.getEmailId().equals(emailId);
                    assert response.getStatus() == EmailStatus.QUEUED;
                });

        verify(emailService).getEmailStatus(emailId);
    }

    @Test
    @DisplayName("GET /api/emails/status/{emailId} - Email non trouvé")
    void testGetEmailStatus_NotFound() {
        // ARRANGE: Service retourne vide
        when(emailService.getEmailStatus("non-existent-id"))
                .thenReturn(Mono.empty());

        // ACT & ASSERT: Tester le cas non trouvé
        webTestClient.get()
                .uri("/api/emails/status/non-existent-id")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("GET /api/emails/status/bulk - Récupération de statuts multiples")
    void testGetBulkEmailStatus_Success() {
        // ARRANGE: Plusieurs réponses
        List<String> emailIds = List.of("id1", "id2", "id3");
        EmailResponse response1 = EmailResponse.builder().emailId("id1").status(EmailStatus.SENT).build();
        EmailResponse response2 = EmailResponse.builder().emailId("id2").status(EmailStatus.FAILED).build();
        EmailResponse response3 = EmailResponse.builder().emailId("id3").status(EmailStatus.QUEUED).build();

        when(emailService.getBulkEmailStatus(emailIds))
                .thenReturn(Flux.just(response1, response2, response3));

        // ACT & ASSERT: Tester la récupération en lot
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/emails/status/bulk")
                        .queryParam("emailIds", "id1", "id2", "id3")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EmailResponse.class)
                .hasSize(3);

        verify(emailService).getBulkEmailStatus(emailIds);
    }

    @Test
    @DisplayName("Test de la réactivité des endpoints avec StepVerifier")
    void testReactiveEndpoints() {
        // ARRANGE
        when(emailService.processEmailRequest(any(EmailRequest.class)))
                .thenReturn(Mono.just(mockEmailResponse));

        // ACT: Appeler directement la méthode du controller
        Mono<org.springframework.http.ResponseEntity<EmailResponse>> result = 
                emailController.sendEmail(validEmailRequest);

        // ASSERT: Vérifier le comportement réactif
        StepVerifier.create(result)
                .expectNextMatches(responseEntity -> 
                        responseEntity.getStatusCode().is2xxSuccessful() &&
                        responseEntity.getBody().getEmailId().equals("test-email-id"))
                .verifyComplete();
    }
}
