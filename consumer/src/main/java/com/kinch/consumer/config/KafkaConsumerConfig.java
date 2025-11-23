package com.kinch.consumer.config;

import com.kinch.common.serializer.JsonDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka消费者配置类
 * 展示不同类型的消费者配置
 */
@Configuration
public class KafkaConsumerConfig {
    
    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;
    
    /**
     * 基础消费者工厂 - 用于String类型消息
     */
    @Bean
    public ConsumerFactory<String, String> stringConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        
        // 核心配置说明
        // group.id: 消费者组ID，同一组内的消费者共享消费进度
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "default-consumer-group");
        
        // auto.offset.reset: earliest-从最早开始消费，latest-从最新开始消费，none-如果没有偏移量则抛异常
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        // enable.auto.commit: 是否自动提交偏移量
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // 手动提交更可靠
        
        // max.poll.records: 单次poll最多拉取的消息数量
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        
        // max.poll.interval.ms: 两次poll的最大间隔时间，超过则认为消费者失效
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5分钟
        
        // session.timeout.ms: 会话超时时间，超过则触发重平衡
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10000); // 10秒
        
        // heartbeat.interval.ms: 心跳间隔时间
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000); // 3秒
        
        return new DefaultKafkaConsumerFactory<>(props);
    }
    
    /**
     * 基础消费者监听容器工厂
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stringConsumerFactory());
        
        // 手动提交模式
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        
        // 并发数：同一消费者组内的并发线程数
        factory.setConcurrency(3);
        
        return factory;
    }
    
    /**
     * 批量消费监听容器工厂
     * 核心知识点：批量消费可以提高吞吐量
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> batchKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stringConsumerFactory());
        
        // 开启批量消费
        factory.setBatchListener(true);
        
        // 手动提交
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        return factory;
    }
    
    /**
     * 事务消费者工厂
     * 核心知识点：配合事务生产者使用，实现端到端的精确一次语义
     */
    @Bean
    public ConsumerFactory<String, String> transactionalConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "transactional-consumer-group");
        
        // 事务隔离级别：read_committed只读取已提交的事务消息
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        return new DefaultKafkaConsumerFactory<>(props);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> transactionalKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(transactionalConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
    
    /**
     * JSON消费者工厂
     */
    @Bean
    public ConsumerFactory<String, Object> jsonConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "json-consumer-group");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        return new DefaultKafkaConsumerFactory<>(props);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> jsonKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(jsonConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}

