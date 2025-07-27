package org.project.emailservice;

import org.junit.jupiter.api.Test;
import org.project.emailservice.config.TestConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=" +
    "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration," +
    "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
    "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration," +
    "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration," +
    "org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration"
})
class EmailServiceApplicationTests {

    @Test
    void contextLoads() {
        // This test verifies that the Spring Boot application context loads successfully
        // without requiring external dependencies like Redis, RabbitMQ, or SMTP servers
    }

}
