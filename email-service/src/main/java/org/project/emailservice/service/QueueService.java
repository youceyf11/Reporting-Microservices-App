package org.project.emailservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.emailservice.dto.EmailRequest;
import org.project.emailservice.dto.EmailMessagePayload;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Service de gestion des files d'attente RabbitMQ.
 * Cette classe utilise RabbitTemplate pour envoyer des messages à RabbitMQ.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QueueService {

    @Value("${rabbitmq.routing.key.name}")
    private String routingKey;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    private final RabbitTemplate rabbitTemplate;

    /**
     * Met en file d'attente un email pour un traitement asynchrone.
     *
     * @param emailRequest Les détails de l'email à envoyer.
     * @param emailId L'ID unique de l'email pour le suivi.
     */
    public void queueEmail(EmailRequest emailRequest, String emailId) {
        EmailMessagePayload payload = new EmailMessagePayload(emailId, emailRequest);
        log.info("Sending email payload to queue for emailId: {}", emailId);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, payload);
    }
}