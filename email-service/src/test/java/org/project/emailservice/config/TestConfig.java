package org.project.emailservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.project.emailservice.entity.EmailLog;
import org.project.emailservice.service.ProviderService;
import org.project.emailservice.service.QueueService;
import org.project.emailservice.service.TemplateService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.Sender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * Configuration de test pour les tests d'intégration.
 * Fournit des mocks pour toutes les dépendances externes afin d'isoler les tests.
 * Cette configuration remplace les services par des mocks pour un contrôle total.
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        return Mockito.mock(JavaMailSender.class);
    }

    @Bean
    @Primary
    public TemplateService templateService() {
        return Mockito.mock(TemplateService.class, Mockito.withSettings().lenient());
    }

    @Bean
    @Primary
    public ProviderService providerService() {
        return Mockito.mock(ProviderService.class, Mockito.withSettings().lenient());
    }

    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate() {
        return Mockito.mock(RabbitTemplate.class);
    }

    @Bean
    @Primary
    public Sender sender() {
        return Mockito.mock(Sender.class);
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public ReactiveRedisTemplate<String, EmailLog> reactiveRedisTemplate() {
        ReactiveRedisTemplate<String, EmailLog> redisTemplate = Mockito.mock(ReactiveRedisTemplate.class);
        ReactiveValueOperations<String, EmailLog> valueOps = Mockito.mock(ReactiveValueOperations.class);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(valueOps.set(anyString(), any(EmailLog.class))).thenReturn(Mono.just(true));
        lenient().when(valueOps.get(anyString())).thenReturn(Mono.empty());
        return redisTemplate;
    }

    @Bean
    @Primary
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplateObject() {
        ReactiveRedisTemplate<String, Object> redisTemplate = Mockito.mock(ReactiveRedisTemplate.class);
        ReactiveValueOperations<String, Object> valueOperations = Mockito.mock(ReactiveValueOperations.class);
        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        return redisTemplate;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}