package com.kinch.producer.config;

import com.kinch.common.serializer.JsonSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka生产者配置类
 * 展示不同类型的生产者配置
 */
@Configuration
public class KafkaProducerConfig {
    
    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;
    
    /**
     * 基础生产者配置 - 用于String类型消息
     */
    @Bean
    public ProducerFactory<String, String> stringProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        // 核心配置说明
        // acks: 0-不等待确认，1-等待Leader确认，all/-1-等待所有ISR确认
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        
        // retries: 重试次数，配合幂等性使用
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        
        // 批量发送配置 - 提升吞吐量
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // 16KB
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10); // 等待10ms收集更多消息
        
        // 压缩配置 - 减少网络传输
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        
        // 缓冲区大小
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(stringProducerFactory());
    }
    
    /**
     * JSON生产者配置 - 用于对象类型消息
     */
    @Bean
    public ProducerFactory<String, Object> jsonProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    @Bean
    public KafkaTemplate<String, Object> jsonKafkaTemplate() {
        return new KafkaTemplate<>(jsonProducerFactory());
    }
    
    /**
     * 幂等性生产者配置 - 保证消息不重复
     * 核心特性：即使重试，也不会产生重复消息
     */
    @Bean
    public ProducerFactory<String, String> idempotentProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        // 开启幂等性 - 这是关键配置
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        // 幂等性要求的配置
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // 必须是all
        configProps.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE); // 可以设置很大
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5); // <=5
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    @Bean
    public KafkaTemplate<String, String> idempotentKafkaTemplate() {
        return new KafkaTemplate<>(idempotentProducerFactory());
    }
    
    /**
     * 事务生产者配置 - 保证跨分区、跨Topic的原子性
     * 核心特性：要么全部成功，要么全部失败
     */
    @Bean
    public ProducerFactory<String, String> transactionalProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        // 事务配置 - 必须设置事务ID前缀
        configProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "tx-producer-");
        
        // 事务自动开启幂等性
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        
        DefaultKafkaProducerFactory<String, String> factory = new DefaultKafkaProducerFactory<>(configProps);
        return factory;
    }
    
    @Bean
    public KafkaTemplate<String, String> transactionalKafkaTemplate() {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(transactionalProducerFactory());
        return template;
    }
}

