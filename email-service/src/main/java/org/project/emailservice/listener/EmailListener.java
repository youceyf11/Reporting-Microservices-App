package org.project.emailservice.listener;

import org.project.emailservice.dto.EmailMessagePayload;
import org.project.emailservice.enums.EmailStatus;
import org.project.emailservice.service.EmailService;
import org.project.emailservice.service.ProviderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
/*
 *  Service de gestion des files d'attente RabbitMQ de manière réactive.
 * 
 *  Cette classe utilise reactor-rabbitmq pour une intégration 100% non-bloquante
 *  avec RabbitMQ.
 * 
 *  @author Votre nom
 *  @since 1.0
 */
public class EmailListener {

    private final ProviderService providerService;
    private final EmailService emailService;

    @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void handleEmailMessage(EmailMessagePayload payload) {
        String emailId = payload.getEmailId();
        log.info("Processing email {} from queue.", emailId);

        try {
            // This is the actual (potentially long) call to send the email
            providerService.sendEmail(payload.getRequest()).block(); 
            log.info("Email {} sent successfully.", emailId);
            // Update status in Redis on success
            emailService.updateEmailStatus(emailId, EmailStatus.SENT, null).subscribe();

        } catch (Exception e) {
            log.error("Failed to send email {}: {}", emailId, e.getMessage(), e);
            // Update status in Redis on failure
            emailService.updateEmailStatus(emailId, EmailStatus.FAILED, e.getMessage()).subscribe();
        }
    }
}