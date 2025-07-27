package org.project.emailservice.config;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.SenderOptions;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.ReceiverOptions;
import reactor.rabbitmq.RabbitFlux;

/**
 * Configuration pour RabbitMQ réactif.
 * 
 * Cette configuration crée les beans Sender et Receiver nécessaires
 * pour une communication 100% non-bloquante avec RabbitMQ.
 * 
 * @author Votre nom
 * @since 1.0
 */
@Configuration
public class RabbitMQReactiveConfig {

    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String EMAIL_HIGH = "email.high";
    public static final String EMAIL_NORMAL = "email.normal";
    public static final String EMAIL_LOW = "email.low";

    @Value("${spring.rabbitmq.host:localhost}")
    private String host;

    @Value("${spring.rabbitmq.port:5672}")
    private int port;

    @Value("${spring.rabbitmq.username:guest}")
    private String username;

    @Value("${spring.rabbitmq.password:guest}")
    private String password;

    /**
     * Crée une ConnectionFactory configurée pour RabbitMQ.
     * 
     * @return ConnectionFactory configurée avec les paramètres de connexion
     */
    private ConnectionFactory createConnectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        
        // Configuration pour la résilience
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(5000); // 5 secondes
        
        return factory;
    }

    /**
     * Bean Sender réactif pour publier des messages vers RabbitMQ.
     * 
     * Le Sender permet de publier des messages de manière non-bloquante
     * et s'intègre parfaitement dans les chaînes réactives.
     * 
     * @return Sender configuré et prêt à l'emploi
     */
    @Bean
    public Sender reactiveSender() {
        SenderOptions senderOptions = new SenderOptions()
            .connectionFactory(createConnectionFactory())
            .resourceManagementScheduler(reactor.core.scheduler.Schedulers.boundedElastic());
            
        return RabbitFlux.createSender(senderOptions);
    }

    /**
     * Bean Receiver réactif pour consommer des messages depuis RabbitMQ.
     * 
     * Le Receiver permet de consommer des messages de manière non-bloquante
     * sous forme de Flux réactif.
     * 
     * @return Receiver configuré et prêt à l'emploi
     */
    @Bean
    public Receiver reactiveReceiver() {
        ReceiverOptions receiverOptions = new ReceiverOptions()
            .connectionFactory(createConnectionFactory())
            .connectionSubscriptionScheduler(reactor.core.scheduler.Schedulers.boundedElastic());
            
        return RabbitFlux.createReceiver(receiverOptions);
    }
}