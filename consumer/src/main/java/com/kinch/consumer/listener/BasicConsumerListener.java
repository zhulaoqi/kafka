package com.kinch.consumer.listener;

import com.kinch.common.constant.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 基础消费者监听器
 * 演示基本的消息消费方式
 */
@Slf4j
@Component
public class BasicConsumerListener {
    
    /**
     * 1. 基础消费 - 自动消费，手动提交
     * 
     * 核心知识点：
     * - @KafkaListener注解标记消费者方法
     * - topics指定要消费的Topic
     * - groupId指定消费者组
     * - ConsumerRecord包含消息的完整信息
     * - Acknowledgment用于手动提交偏移量
     */
    @KafkaListener(
        topics = KafkaConstants.TOPIC_SIMPLE,
        groupId = KafkaConstants.GROUP_SIMPLE,
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeSimpleMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            log.info("【基础消费者】收到消息 - Topic: {}, Partition: {}, Offset: {}, Key: {}, Value: {}", 
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                record.value());
            
            // 处理消息
            processMessage(record);
            
            // 手动提交偏移量
            // 只有成功处理后才提交，保证至少一次语义（At Least Once）
            ack.acknowledge();
            log.debug("【基础消费者】偏移量提交成功 - Offset: {}", record.offset());
            
        } catch (Exception e) {
            log.error("【基础消费者】消息处理失败 - Offset: {}", record.offset(), e);
            // 不提交偏移量，下次会重新消费这条消息
            // 注意：需要实现重试机制或死信队列，避免无限重试
        }
    }
    
    /**
     * 2. 消费带Header的消息
     * 核心知识点：Header可以携带元数据，用于链路追踪、消息标识等
     */
    @KafkaListener(
        topics = KafkaConstants.TOPIC_USER,
        groupId = KafkaConstants.GROUP_USER,
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeWithHeaders(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            log.info("【Header消费者】收到消息 - Key: {}, Value: {}", record.key(), record.value());
            
            // 读取Header信息
            for (Header header : record.headers()) {
                String key = header.key();
                String value = new String(header.value(), StandardCharsets.UTF_8);
                log.info("【Header消费者】Header - {}: {}", key, value);
                
                // 可以根据Header做特殊处理
                if (KafkaConstants.HEADER_TRACE_ID.equals(key)) {
                    log.info("【Header消费者】TraceId: {}", value);
                }
            }
            
            // 处理消息
            processMessage(record);
            
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("【Header消费者】消息处理失败", e);
        }
    }
    
    /**
     * 3. 多Topic消费
     * 核心知识点：一个消费者可以同时消费多个Topic
     */
    @KafkaListener(
        topics = {KafkaConstants.TOPIC_SIMPLE, KafkaConstants.TOPIC_USER},
        groupId = "multi-topic-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMultipleTopics(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            log.info("【多Topic消费者】收到消息 - Topic: {}, Partition: {}, Key: {}", 
                record.topic(), record.partition(), record.key());
            
            // 可以根据Topic做不同处理
            switch (record.topic()) {
                case KafkaConstants.TOPIC_SIMPLE:
                    log.info("【多Topic消费者】处理Simple消息");
                    break;
                case KafkaConstants.TOPIC_USER:
                    log.info("【多Topic消费者】处理User消息");
                    break;
                default:
                    log.warn("【多Topic消费者】未知Topic: {}", record.topic());
            }
            
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("【多Topic消费者】消息处理失败", e);
        }
    }
    
    /**
     * 4. 指定分区消费
     * 核心知识点：可以指定消费特定分区
     * 注意：指定分区后，消费者不会参与重平衡
     */
    @KafkaListener(
        topicPartitions = @org.springframework.kafka.annotation.TopicPartition(
            topic = KafkaConstants.TOPIC_SIMPLE,
            partitions = {"0", "1"}
        ),
        groupId = "partition-specific-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeSpecificPartitions(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            log.info("【指定分区消费者】收到消息 - Partition: {}, Offset: {}, Key: {}", 
                record.partition(), record.offset(), record.key());
            
            processMessage(record);
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("【指定分区消费者】消息处理失败", e);
        }
    }
    
    /**
     * 5. 从指定偏移量开始消费
     * 核心知识点：可以指定初始偏移量
     */
    @KafkaListener(
            topicPartitions = @org.springframework.kafka.annotation.TopicPartition(
                    topic = KafkaConstants.TOPIC_SIMPLE,
                    partitionOffsets = {
                            @org.springframework.kafka.annotation.PartitionOffset(
                                    partition = "2",
                                    initialOffset = "0" // 从offset=0开始消费
                            )
                    }
            ),
            groupId = "offset-specific-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeFromSpecificOffset(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            log.info("【指定偏移量消费者】收到消息 - Partition: {}, Offset: {}", 
                record.partition(), record.offset());
            
            processMessage(record);
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("【指定偏移量消费者】消息处理失败", e);
        }
    }
    
    /**
     * 消息处理逻辑
     */
    private void processMessage(ConsumerRecord<String, String> record) {
        // 模拟业务处理
        String message = record.value();
        log.debug("【消息处理】开始处理: {}", message);
        
        // 这里可以添加实际的业务逻辑
        // 例如：存储到数据库、调用其他服务等
        
        log.debug("【消息处理】处理完成: {}", message);
    }
}

