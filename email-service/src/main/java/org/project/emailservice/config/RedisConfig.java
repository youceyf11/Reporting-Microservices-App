package org.project.emailservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.project.emailservice.entity.EmailLog;

@Configuration
public class RedisConfig {
    
    
    public ReactiveRedisTemplate<String, EmailLog> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        
        Jackson2JsonRedisSerializer<EmailLog> serializer = 
                new Jackson2JsonRedisSerializer<>(EmailLog.class);
        
        RedisSerializationContext<String, EmailLog> context = 
                RedisSerializationContext.<String, EmailLog>newSerializationContext()
                        .key(new StringRedisSerializer())
                        .value(serializer)
                        .build();
        
        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
}
