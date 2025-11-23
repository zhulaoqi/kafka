package com.kinch.consumer.listener;

import com.kinch.common.constant.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 批量消费监听器
 * 
 * 核心知识点：
 * 1. 批量消费可以提高吞吐量，减少网络开销
 * 2. 适合需要批量处理的场景，如批量入库
 * 3. 需要注意内存占用和处理时间
 * 4. 批量消费失败的处理策略更复杂
 */
@Slf4j
@Component
public class BatchConsumerListener {
    
    /**
     * 批量消费消息
     * 
     * @param records 一批消息记录
     * @param ack 确认对象
     */
    @KafkaListener(
        topics = KafkaConstants.TOPIC_SIMPLE,
        groupId = "batch-consumer-group",
        containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consumeBatch(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        try {
            log.info("【批量消费者】收到批量消息 - 数量: {}", records.size());
            
            // 记录批次的第一条和最后一条消息的偏移量
            if (!records.isEmpty()) {
                ConsumerRecord<String, String> first = records.get(0);
                ConsumerRecord<String, String> last = records.get(records.size() - 1);
                
                log.info("【批量消费者】批次范围 - Topic: {}, Partition: {}, Offset: {} ~ {}", 
                    first.topic(), first.partition(), first.offset(), last.offset());
            }
            
            // 批量处理消息
            processBatch(records);
            
            // 批量提交偏移量
            // 注意：批量消费时，要么全部成功，要么全部失败
            ack.acknowledge();
            log.info("【批量消费者】批量处理成功，偏移量已提交");
            
        } catch (Exception e) {
            log.error("【批量消费者】批量处理失败", e);
            // 批量处理失败时的策略：
            // 1. 不提交偏移量，下次重新消费整个批次
            // 2. 或者转为单条处理，找出失败的消息
            handleBatchFailure(records, e);
        }
    }
    
    /**
     * 批量处理消息
     */
    private void processBatch(List<ConsumerRecord<String, String>> records) {
        log.info("【批量处理】开始处理 {} 条消息", records.size());
        
        long startTime = System.currentTimeMillis();
        
        // 示例1：批量入库
        // batchInsertToDatabase(records);
        
        // 示例2：批量调用外部API
        // batchCallExternalApi(records);
        
        // 这里简化为遍历处理
        for (ConsumerRecord<String, String> record : records) {
            log.debug("【批量处理】处理消息 - Offset: {}, Key: {}, Value: {}", 
                record.offset(), record.key(), record.value());
        }
        
        long endTime = System.currentTimeMillis();
        log.info("【批量处理】完成，耗时: {} ms, 吞吐量: {} msg/s", 
            (endTime - startTime),
            records.size() * 1000.0 / (endTime - startTime));
    }
    
    /**
     * 批量处理失败的处理策略
     */
    private void handleBatchFailure(List<ConsumerRecord<String, String>> records, Exception e) {
        log.error("【批量处理失败】开始处理失败批次，尝试单条处理");
        
        // 策略1：转为单条处理，找出失败的消息
        int successCount = 0;
        int failureCount = 0;
        
        for (ConsumerRecord<String, String> record : records) {
            try {
                // 尝试单独处理每条消息
                processSingleRecord(record);
                successCount++;
            } catch (Exception ex) {
                failureCount++;
                log.error("【批量处理失败】单条处理失败 - Offset: {}, Key: {}", 
                    record.offset(), record.key(), ex);
                
                // 将失败的消息发送到死信队列
                sendToDeadLetterQueue(record, ex);
            }
        }
        
        log.info("【批量处理失败】单条处理完成 - 成功: {}, 失败: {}", successCount, failureCount);
    }
    
    /**
     * 单条消息处理
     */
    private void processSingleRecord(ConsumerRecord<String, String> record) {
        // 实际的业务处理逻辑
        log.debug("【单条处理】处理消息: {}", record.value());
    }
    
    /**
     * 发送到死信队列
     */
    private void sendToDeadLetterQueue(ConsumerRecord<String, String> record, Exception e) {
        log.warn("【死信队列】发送失败消息 - Topic: {}, Offset: {}, Error: {}", 
            record.topic(), record.offset(), e.getMessage());
        
        // 实际应该使用KafkaTemplate发送到DLQ
        // kafkaTemplate.send(KafkaConstants.TOPIC_DLQ, record.key(), record.value());
    }
}

