package com.kinch.producer.service;

import com.kinch.common.constant.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * 幂等性生产者服务
 * 
 * 核心知识点：
 * 1. 幂等性保证：在网络异常重试的情况下，Kafka保证消息不重复
 * 2. 实现原理：Producer会为每个消息分配一个递增的序列号（Sequence Number）
 *              Broker会记录每个Producer的最大序列号，重复消息会被去重
 * 3. 作用范围：单个Producer、单个分区、单次会话（重启后失效）
 * 4. 配置要求：
 *    - enable.idempotence=true
 *    - acks=all
 *    - retries > 0
 *    - max.in.flight.requests.per.connection <= 5
 * 
 * 使用场景：
 * - 需要精确一次语义（Exactly Once）的场景
 * - 防止网络抖动导致的重复消息
 */
@Slf4j
@Service
public class IdempotentProducerService {
    
    @Autowired
    @Qualifier("idempotentKafkaTemplate")
    private KafkaTemplate<String, String> idempotentKafkaTemplate;
    
    /**
     * 发送幂等性消息
     * 即使发生重试，也不会产生重复消息
     */
    public void sendIdempotentMessage(String key, String message) {
        log.info("【幂等性生产者】准备发送 - key: {}, message: {}", key, message);
        
        idempotentKafkaTemplate.send(KafkaConstants.TOPIC_SIMPLE, key, message)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    var metadata = result.getRecordMetadata();
                    log.info("【幂等性生产者】发送成功 - partition: {}, offset: {}", 
                        metadata.partition(), metadata.offset());
                } else {
                    log.error("【幂等性生产者】发送失败", ex);
                }
            });
    }
    
    /**
     * 批量发送幂等性消息
     * 展示幂等性在批量发送场景下的作用
     */
    public void sendBatch(String keyPrefix, int count) {
        log.info("【幂等性生产者】开始批量发送 {} 条消息", count);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < count; i++) {
            String key = keyPrefix + "-" + i;
            String message = "幂等性消息-" + i + "-时间戳-" + System.currentTimeMillis();
            
            idempotentKafkaTemplate.send(KafkaConstants.TOPIC_SIMPLE, key, message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("【幂等性生产者】批量发送失败 - key: {}", key, ex);
                    }
                });
        }
        
        long endTime = System.currentTimeMillis();
        log.info("【幂等性生产者】批量发送完成，耗时: {} ms", (endTime - startTime));
    }
    
    /**
     * 模拟网络异常场景
     * 说明：即使在网络异常导致重试的情况下，幂等性也能保证消息不重复
     * 
     * 实际测试方法：
     * 1. 发送消息
     * 2. 在发送过程中断开网络连接
     * 3. 重新连接网络
     * 4. 观察Broker端是否有重复消息
     */
    public void sendWithRetryScenario(String key, String message) {
        log.info("【幂等性生产者-重试场景】发送消息 - key: {}", key);
        
        // 注意：幂等性配置中已经设置了 retries=Integer.MAX_VALUE
        // 所以即使遇到可重试的异常，Producer也会自动重试
        
        idempotentKafkaTemplate.send(KafkaConstants.TOPIC_SIMPLE, key, message)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    var metadata = result.getRecordMetadata();
                    log.info("【幂等性生产者-重试场景】最终发送成功 - offset: {}", 
                        metadata.offset());
                } else {
                    log.error("【幂等性生产者-重试场景】最终发送失败", ex);
                }
            });
    }
}

