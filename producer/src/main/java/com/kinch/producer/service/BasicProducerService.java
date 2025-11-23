package com.kinch.producer.service;

import com.kinch.common.constant.KafkaConstants;
import com.kinch.common.util.KafkaUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * 基础生产者服务
 * 演示基本的消息发送方式
 */
@Slf4j
@Service
public class BasicProducerService {
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    /**
     * 1. 发后即忘（Fire and Forget）
     * 特点：最快，但可能丢消息
     * 场景：日志收集等对可靠性要求不高的场景
     */
    public void sendFireAndForget(String message) {
        kafkaTemplate.send(KafkaConstants.TOPIC_SIMPLE, message);
        log.info("【发后即忘】发送消息: {}", message);
    }
    
    /**
     * 2. 同步发送（Synchronous Send）
     * 特点：最慢，但最可靠
     * 场景：对可靠性要求极高的场景
     */
    public void sendSync(String message) {
        try {
            SendResult<String, String> result = kafkaTemplate
                .send(KafkaConstants.TOPIC_SIMPLE, message)
                .get(); // 阻塞等待结果
            
            RecordMetadata metadata = result.getRecordMetadata();
            log.info("【同步发送】发送成功 - topic: {}, partition: {}, offset: {}", 
                metadata.topic(), metadata.partition(), metadata.offset());
                
        } catch (Exception e) {
            log.error("【同步发送】发送失败", e);
            throw new RuntimeException("消息发送失败", e);
        }
    }
    
    /**
     * 3. 异步发送（Asynchronous Send）
     * 特点：高性能，可靠性可控
     * 场景：大部分生产场景的最佳选择
     */
    public void sendAsync(String message) {
        CompletableFuture<SendResult<String, String>> future = 
            kafkaTemplate.send(KafkaConstants.TOPIC_SIMPLE, message);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                RecordMetadata metadata = result.getRecordMetadata();
                log.info("【异步发送】发送成功 - topic: {}, partition: {}, offset: {}, timestamp: {}", 
                    metadata.topic(), 
                    metadata.partition(), 
                    metadata.offset(),
                    metadata.timestamp());
            } else {
                log.error("【异步发送】发送失败: {}", message, ex);
            }
        });
        
        log.info("【异步发送】消息已提交到缓冲区: {}", message);
    }
    
    /**
     * 4. 带Key的发送
     * 核心知识点：相同Key的消息会发送到同一分区，保证顺序性
     * 场景：需要保证消息顺序的场景，如用户操作日志
     */
    public void sendWithKey(String key, String message) {
        kafkaTemplate.send(KafkaConstants.TOPIC_SIMPLE, key, message)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    RecordMetadata metadata = result.getRecordMetadata();
                    log.info("【带Key发送】成功 - key: {}, partition: {}, offset: {}", 
                        key, metadata.partition(), metadata.offset());
                } else {
                    log.error("【带Key发送】失败 - key: {}", key, ex);
                }
            });
    }
    
    /**
     * 5. 指定分区发送
     * 核心知识点：可以手动控制消息发送到哪个分区
     * 场景：需要精确控制分区的场景
     */
    public void sendToPartition(int partition, String message) {
        ProducerRecord<String, String> record = new ProducerRecord<>(
            KafkaConstants.TOPIC_SIMPLE,
            partition,
            null,
            message
        );
        
        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex == null) {
                RecordMetadata metadata = result.getRecordMetadata();
                log.info("【指定分区发送】成功 - 目标分区: {}, 实际分区: {}, offset: {}", 
                    partition, metadata.partition(), metadata.offset());
            } else {
                log.error("【指定分区发送】失败 - 目标分区: {}", partition, ex);
            }
        });
    }
    
    /**
     * 6. 带Header的发送
     * 核心知识点：Header可以携带元数据，不影响消息体
     * 场景：链路追踪、消息标识、业务标签等
     */
    public void sendWithHeaders(String key, String message) {
        ProducerRecord<String, String> record = new ProducerRecord<>(
            KafkaConstants.TOPIC_SIMPLE,
            key,
            message
        );
        
        // 添加自定义Header
        String messageId = KafkaUtils.generateMessageId();
        String traceId = KafkaUtils.generateTraceId();
        
        record.headers().add(KafkaConstants.HEADER_MESSAGE_ID, 
            messageId.getBytes(StandardCharsets.UTF_8));
        record.headers().add(KafkaConstants.HEADER_TRACE_ID, 
            traceId.getBytes(StandardCharsets.UTF_8));
        record.headers().add(KafkaConstants.HEADER_SOURCE, 
            "basic-producer".getBytes(StandardCharsets.UTF_8));
        record.headers().add(KafkaConstants.HEADER_TIMESTAMP, 
            String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
        
        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("【带Header发送】成功 - messageId: {}, traceId: {}", messageId, traceId);
            } else {
                log.error("【带Header发送】失败 - messageId: {}", messageId, ex);
            }
        });
    }
}

