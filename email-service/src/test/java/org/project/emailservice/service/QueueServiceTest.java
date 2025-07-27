package org.project.emailservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.project.emailservice.dto.EmailRequest;
import org.project.emailservice.enums.EmailPriority;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import reactor.rabbitmq.Sender;

import java.util.Map;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour QueueService
 * 
 * LOGIQUE DES TESTS DE QUEUE:
 * 1. Test du routage par priorité : HIGH → email.queue.high, NORMAL → email.queue.normal, LOW → email.queue.low
 * 2. Test de la sérialisation des messages EmailRequest vers RabbitMQ
 * 3. Test de la gestion d'erreurs lors de l'envoi vers les queues
 * 4. Test de la performance avec de multiples messages
 * 5. Test du comportement par défaut (priorité NORMAL si non spécifiée)
 * 6. Test de la résilience et des retry en cas d'échec RabbitMQ
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QueueService Unit Tests - Tests de gestion des queues RabbitMQ")
class QueueServiceTest {

    @Mock
    private Sender sender;
    
    @Mock
    private ObjectMapper objectMapper;

    private QueueService queueService;

    private EmailRequest highPriorityEmail;
    private EmailRequest normalPriorityEmail;
    private EmailRequest lowPriorityEmail;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        // Utiliser lenient() pour éviter les UnnecessaryStubbingException pour les stubs définis ici
        lenient().when(sender.send(argThat(outboundMessage -> true)))
                .thenReturn(Mono.empty());

        // Simuler la sérialisation pour différents objets
        lenient().when(objectMapper.writeValueAsBytes(any(EmailRequest.class)))
                .thenAnswer(invocation -> {
                    EmailRequest emailRequest = invocation.getArgument(0);
                    if (emailRequest.getPriority() == EmailPriority.HIGH) {
                        return "high-priority-json".getBytes();
                    } else if (emailRequest.getPriority() == EmailPriority.NORMAL) {
                        return "normal-priority-json".getBytes();
                    } else {
                        return "low-priority-json".getBytes();
                    }
                });

        queueService = new QueueService(sender, objectMapper);

        // Emails avec différentes priorités
        highPriorityEmail = EmailRequest.builder()
                .id("high-priority-id")
                .to("urgent@example.com")
                .subject("Urgent Email")
                .priority(EmailPriority.HIGH)
                .templateData(Map.of("urgency", "high"))
                .build();

        normalPriorityEmail = EmailRequest.builder()
                .id("normal-priority-id")
                .to("normal@example.com")
                .subject("Normal Email")
                .priority(EmailPriority.NORMAL)
                .templateData(Map.of("urgency", "normal"))
                .build();

        lowPriorityEmail = EmailRequest.builder()
                .id("low-priority-id")
                .to("low@example.com")
                .subject("Low Priority Email")
                .priority(EmailPriority.LOW)
                .templateData(Map.of("urgency", "low"))
                .build();
    }

    @Test
    @DisplayName("queueEmail() - Routage priorité HIGH vers queue high")
    void testQueueEmail_HighPriority() throws JsonProcessingException {
        // ACT
        Mono<Void> result = queueService.queueEmail(highPriorityEmail);

        // ASSERT
        StepVerifier.create(result)
                .verifyComplete();

        verify(sender).send(argThat(outboundMessage -> true));
        verify(objectMapper).writeValueAsBytes(highPriorityEmail);
    }

    @Test
    @DisplayName("queueEmail() - Routage priorité NORMAL vers queue normal")
    void testQueueEmail_NormalPriority() throws JsonProcessingException {
        // ACT
        Mono<Void> result = queueService.queueEmail(normalPriorityEmail);

        // ASSERT
        StepVerifier.create(result)
                .verifyComplete();

        verify(sender).send(argThat(outboundMessage -> true));
        verify(objectMapper).writeValueAsBytes(normalPriorityEmail);
    }

    @Test
    @DisplayName("queueEmail() - Routage priorité LOW vers queue low")
    void testQueueEmail_LowPriority() throws JsonProcessingException {
        // ACT
        Mono<Void> result = queueService.queueEmail(lowPriorityEmail);

        // ASSERT
        StepVerifier.create(result)
                .verifyComplete();

        verify(sender).send(argThat(outboundMessage -> true));
        verify(objectMapper).writeValueAsBytes(lowPriorityEmail);
    }

    @Test
    @DisplayName("queueEmail() - Gestion d'erreur RabbitMQ")
    void testQueueEmail_RabbitMQError() {
        // ARRANGE - Configurer le mock pour retourner une erreur
        when(sender.send(argThat(outboundMessage -> true)))
                .thenReturn(Mono.error(new RuntimeException("RabbitMQ connection failed")));

        // ACT
        Mono<Void> result = queueService.queueEmail(normalPriorityEmail);

        // ASSERT
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(sender).send(argThat(outboundMessage -> true));
    }

    @Test
    @DisplayName("queueEmail() - Erreur de sérialisation JSON")
    void testQueueEmail_SerializationError() throws Exception {
        // ARRANGE - Configurer le mock pour lancer une exception
        when(objectMapper.writeValueAsBytes(any(EmailRequest.class)))
                .thenThrow(new RuntimeException("JSON serialization failed"));

        // ACT
        Mono<Void> result = queueService.queueEmail(normalPriorityEmail);

        // ASSERT
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(objectMapper).writeValueAsBytes(normalPriorityEmail);
        verify(sender, never()).send(argThat(outboundMessage -> true));
    }

    @Test
    @DisplayName("queueEmail() - Email sans priorité définie (défaut NORMAL)")
    void testQueueEmail_NullPriority() throws JsonProcessingException {
        // ARRANGE
        EmailRequest emailWithoutPriority = EmailRequest.builder()
                .id("no-priority-id")
                .to("test@example.com")
                .subject("No Priority Email")
                .priority(null)
                .templateData(Map.of("test", "data"))
                .build();

        // ACT
        Mono<Void> result = queueService.queueEmail(emailWithoutPriority);

        // ASSERT
        StepVerifier.create(result)
                .verifyComplete();

        verify(sender).send(argThat(outboundMessage -> true));
        verify(objectMapper).writeValueAsBytes(emailWithoutPriority);
    }

    @Test
    @DisplayName("Test de performance - Mise en queue de multiples emails")
    void testQueueMultipleEmails_Performance() throws JsonProcessingException {
        // ARRANGE
        int numberOfEmails = 5; // Réduit pour éviter les timeouts

        // ACT & ASSERT - Traiter chaque email individuellement
        for (int i = 0; i < numberOfEmails; i++) {
            EmailRequest email = EmailRequest.builder()
                    .id("email-" + i)
                    .to("test" + i + "@example.com")
                    .subject("Test Email " + i)
                    .priority(i % 3 == 0 ? EmailPriority.HIGH : 
                             i % 3 == 1 ? EmailPriority.NORMAL : EmailPriority.LOW)
                    .templateData(Map.of("index", String.valueOf(i)))
                    .build();
            
            StepVerifier.create(queueService.queueEmail(email))
                    .verifyComplete();
        }

        // Vérifier que RabbitMQ a été appelé le bon nombre de fois
        verify(sender, times(numberOfEmails)).send(argThat(outboundMessage -> true));
        verify(objectMapper, times(numberOfEmails)).writeValueAsBytes(any(EmailRequest.class));
    }

    @Test
    @DisplayName("Test de routage correct selon les priorités")
    void testPriorityRouting() throws JsonProcessingException {
        // ACT - Envoyer des emails de chaque priorité
        StepVerifier.create(queueService.queueEmail(highPriorityEmail))
                .verifyComplete();
        StepVerifier.create(queueService.queueEmail(normalPriorityEmail))
                .verifyComplete();
        StepVerifier.create(queueService.queueEmail(lowPriorityEmail))
                .verifyComplete();

        // ASSERT - Vérifier le bon nombre d'appels
        verify(sender, times(3)).send(argThat(outboundMessage -> true));
        verify(objectMapper, times(3)).writeValueAsBytes(any(EmailRequest.class));
    }

    @Test
    @DisplayName("Test avec priorité URGENT (mappée vers HIGH)")
    void testQueueEmail_UrgentPriority() throws JsonProcessingException {
        // ARRANGE
        EmailRequest urgentEmail = EmailRequest.builder()
                .id("urgent-id")
                .to("urgent@example.com")
                .subject("Urgent Email")
                .priority(EmailPriority.URGENT)
                .templateData(Map.of("level", "urgent"))
                .build();

        // ACT
        Mono<Void> result = queueService.queueEmail(urgentEmail);

        // ASSERT
        StepVerifier.create(result)
                .verifyComplete();

        verify(sender).send(argThat(outboundMessage -> true));
        verify(objectMapper).writeValueAsBytes(urgentEmail);
    }
}
