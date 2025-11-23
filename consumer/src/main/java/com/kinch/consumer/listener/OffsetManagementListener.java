package com.kinch.consumer.listener;

import com.kinch.common.constant.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 偏移量管理监听器
 * 
 * 核心知识点：
 * 1. 偏移量（Offset）：消息在分区中的唯一序号
 * 2. 偏移量管理是保证消息不丢失、不重复的关键
 * 3. 提交策略：
 *    - 自动提交：简单但可能丢消息或重复消费
 *    - 手动提交：更可靠，可以精确控制
 * 4. 偏移量存储：Kafka内部Topic（__consumer_offsets）
 * 5. 重置策略：earliest、latest、none
 * 
 * 消息语义保证：
 * 1. At Most Once（至多一次）：可能丢消息，不重复
 *    - 先提交偏移量，再处理消息
 * 2. At Least Once（至少一次）：不丢消息，可能重复（推荐）
 *    - 先处理消息，再提交偏移量
 * 3. Exactly Once（精确一次）：不丢不重
 *    - 使用事务 + 幂等性
 */
@Slf4j
@Component
public class OffsetManagementListener implements ConsumerSeekAware {
    
    /**
     * 1. 手动提交偏移量 - 同步方式
     * At Least Once语义：先处理，后提交
     */
    @KafkaListener(
        topics = KafkaConstants.TOPIC_SIMPLE,
        groupId = "manual-offset-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeWithManualCommit(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            log.info("【手动提交】收到消息 - Partition: {}, Offset: {}, Key: {}", 
                record.partition(), record.offset(), record.key());
            
            // 1. 先处理消息
            processMessage(record);
            
            // 2. 处理成功后，手动提交偏移量
            ack.acknowledge();
            log.debug("【手动提交】偏移量提交成功 - Offset: {}", record.offset());
            
            // 这样保证了At Least Once语义：
            // - 如果处理失败，不提交偏移量，下次会重新消费
            // - 如果处理成功但提交失败，下次也会重新消费（重复消费但不丢消息）
            
        } catch (Exception e) {
            log.error("【手动提交】消息处理失败 - Offset: {}", record.offset(), e);
            // 不提交偏移量，下次会重新消费
            // 注意：需要实现重试次数限制，避免无限重试
        }
    }
    
    /**
     * 2. 批量提交偏移量
     * 提高性能，但失败时会重复消费更多消息
     */
    @KafkaListener(
        topics = KafkaConstants.TOPIC_USER,
        groupId = "batch-commit-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeWithBatchCommit(ConsumerRecord<String, String> record, 
                                      Acknowledgment ack,
                                      Consumer<?, ?> consumer) {
        try {
            log.info("【批量提交】收到消息 - Partition: {}, Offset: {}", 
                record.partition(), record.offset());
            
            processMessage(record);
            
            // 每处理10条消息提交一次
            if (record.offset() % 10 == 0) {
                ack.acknowledge();
                log.info("【批量提交】批量提交偏移量 - Offset: {}", record.offset());
            }
            
        } catch (Exception e) {
            log.error("【批量提交】消息处理失败", e);
        }
    }
    
    /**
     * 3. 指定偏移量消费
     * 实现ConsumerSeekAware接口，可以在启动时指定偏移量
     */
    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        log.info("【偏移量管理】分区分配完成: {}", assignments);
        
        // 示例：重置到特定偏移量
        // for (TopicPartition partition : assignments.keySet()) {
        //     // 从偏移量100开始消费
        //     callback.seek(partition.topic(), partition.partition(), 100);
        //     log.info("【偏移量管理】重置分区偏移量 - Partition: {}, Offset: 100", partition.partition());
        // }
    }
    
    /**
     * 4. 重置到最早/最新
     */
    @Override
    public void registerSeekCallback(ConsumerSeekCallback callback) {
        log.info("【偏移量管理】注册Seek回调");
        // 可以保存callback，在需要时重置偏移量
    }
    
    /**
     * 演示不同的提交时机
     */
    @KafkaListener(
        topics = KafkaConstants.TOPIC_ORDER,
        groupId = "commit-timing-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void demonstrateCommitTiming(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            log.info("【提交时机】收到消息 - Offset: {}", record.offset());
            
            // 场景1：At Most Once（至多一次）- 可能丢消息
            // ack.acknowledge(); // 先提交
            // processMessage(record); // 后处理
            // 如果处理失败，消息已经提交，会丢失
            
            // 场景2：At Least Once（至少一次）- 可能重复（推荐）
            processMessage(record); // 先处理
            ack.acknowledge(); // 后提交
            // 如果处理成功但提交失败，下次会重复消费，但不会丢失
            
            // 场景3：Exactly Once（精确一次）- 需要事务
            // 使用事务生产者 + read_committed隔离级别
            // 或者实现幂等性业务逻辑
            
        } catch (Exception e) {
            log.error("【提交时机】消息处理失败", e);
            
            // 重试策略
            retryOrSendToDLQ(record, e);
        }
    }
    
    /**
     * 处理消息
     */
    private void processMessage(ConsumerRecord<String, String> record) {
        // 实际业务处理
        log.debug("【消息处理】处理中: {}", record.value());
        
        // 模拟处理可能失败
        // if (record.value().contains("error")) {
        //     throw new RuntimeException("模拟处理失败");
        // }
    }
    
    /**
     * 重试或发送到死信队列
     */
    private void retryOrSendToDLQ(ConsumerRecord<String, String> record, Exception e) {
        log.warn("【重试策略】消息处理失败，考虑重试或DLQ - Offset: {}", record.offset());
        
        // 实际应该：
        // 1. 记录重试次数（可以使用Header记录）
        // 2. 达到最大重试次数后，发送到死信队列
        // 3. 人工介入处理死信队列的消息
    }
}

