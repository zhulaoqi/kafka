package com.kinch.producer.service;

import com.kinch.common.constant.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * 事务生产者服务
 * 
 * 核心知识点：
 * 1. Kafka事务提供跨分区、跨Topic的原子性保证
 * 2. 事务消息：要么全部成功，要么全部失败（回滚）
 * 3. 实现原理：
 *    - Transaction Coordinator：事务协调器
 *    - Transaction Log：事务日志Topic（__transaction_state）
 *    - 两阶段提交协议
 * 4. 配置要求：
 *    - transactional.id：每个事务Producer必须有唯一的事务ID
 *    - enable.idempotence=true：事务自动开启幂等性
 *    - acks=all
 * 5. 事务隔离级别：
 *    - read_uncommitted：可以读到未提交的事务消息（默认）
 *    - read_committed：只能读到已提交的事务消息
 * 
 * 使用场景：
 * - 需要保证多条消息的原子性
 * - 需要实现精确一次语义（Exactly Once Semantics）
 * - 跨多个Topic的数据一致性
 * - 消费-转换-生产的场景（Consume-Transform-Produce）
 */
@Slf4j
@Service
public class TransactionalProducerService {
    
    @Autowired
    @Qualifier("transactionalKafkaTemplate")
    private KafkaTemplate<String, String> transactionalKafkaTemplate;
    
    /**
     * 1. 基础事务发送
     * 演示事务的基本用法
     */
    public void sendInTransaction(String message1, String message2) {
        log.info("【事务生产者】开始事务发送");
        
        // 使用executeInTransaction执行事务
        transactionalKafkaTemplate.executeInTransaction(operations -> {
            // 发送第一条消息
            operations.send(KafkaConstants.TOPIC_SIMPLE, "tx-key-1", message1)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("【事务生产者】消息1发送成功: {}", message1);
                    }
                });
            
            // 发送第二条消息
            operations.send(KafkaConstants.TOPIC_SIMPLE, "tx-key-2", message2)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("【事务生产者】消息2发送成功: {}", message2);
                    }
                });
            
            // 如果没有异常，事务会自动提交
            // 如果抛出异常，事务会自动回滚
            return true;
        });
        
        log.info("【事务生产者】事务提交完成");
    }
    
    /**
     * 2. 跨Topic事务发送
     * 核心知识点：事务可以保证多个Topic的消息原子性
     */
    public void sendCrossTopics(String orderMessage, String userMessage) {
        log.info("【事务生产者-跨Topic】开始事务发送");
        
        transactionalKafkaTemplate.executeInTransaction(operations -> {
            // 发送订单消息到order-topic
            operations.send(KafkaConstants.TOPIC_ORDER, orderMessage);
            log.info("【事务生产者-跨Topic】订单消息已发送");
            
            // 发送用户消息到user-topic
            operations.send(KafkaConstants.TOPIC_USER, userMessage);
            log.info("【事务生产者-跨Topic】用户消息已发送");
            
            // 两条消息要么都成功，要么都失败
            return true;
        });
        
        log.info("【事务生产者-跨Topic】跨Topic事务提交完成");
    }
    
    /**
     * 3. 事务回滚场景
     * 演示当发生异常时，事务会自动回滚
     */
    public void sendWithRollback(String message1, String message2, boolean shouldFail) {
        log.info("【事务生产者-回滚】开始事务发送，shouldFail={}", shouldFail);
        
        try {
            transactionalKafkaTemplate.executeInTransaction(operations -> {
                // 发送第一条消息
                operations.send(KafkaConstants.TOPIC_SIMPLE, message1);
                log.info("【事务生产者-回滚】消息1已发送: {}", message1);
                
                // 模拟业务异常
                if (shouldFail) {
                    log.error("【事务生产者-回滚】模拟业务异常，事务将回滚");
                    throw new RuntimeException("模拟业务异常");
                }
                
                // 发送第二条消息
                operations.send(KafkaConstants.TOPIC_SIMPLE, message2);
                log.info("【事务生产者-回滚】消息2已发送: {}", message2);
                
                return true;
            });
            
            log.info("【事务生产者-回滚】事务提交成功");
            
        } catch (Exception e) {
            log.error("【事务生产者-回滚】事务回滚，所有消息都不会被消费到", e);
        }
    }
    
    /**
     * 4. 批量事务发送
     * 场景：需要保证一批消息的原子性
     */
    public void sendBatchInTransaction(String keyPrefix, int count) {
        log.info("【事务生产者-批量】开始批量事务发送 {} 条消息", count);
        
        long startTime = System.currentTimeMillis();
        
        try {
            transactionalKafkaTemplate.executeInTransaction(operations -> {
                for (int i = 0; i < count; i++) {
                    String key = keyPrefix + "-" + i;
                    String message = "事务消息-" + i;
                    operations.send(KafkaConstants.TOPIC_SIMPLE, key, message);
                }
                return true;
            });
            
            long endTime = System.currentTimeMillis();
            log.info("【事务生产者-批量】批量事务提交成功，耗时: {} ms", (endTime - startTime));
            
        } catch (Exception e) {
            log.error("【事务生产者-批量】批量事务失败，所有消息回滚", e);
        }
    }
    
    /**
     * 5. 复杂业务场景：订单处理
     * 演示实际业务中的事务使用
     * 场景：创建订单需要同时发送订单消息、库存扣减消息、用户积分消息
     */
    public void processOrderInTransaction(String orderId, String userId, String productId, int quantity) {
        log.info("【事务生产者-订单处理】开始处理订单: orderId={}, userId={}, productId={}, quantity={}", 
            orderId, userId, productId, quantity);
        
        try {
            transactionalKafkaTemplate.executeInTransaction(operations -> {
                // 1. 发送订单创建消息
                String orderMessage = String.format(
                    "{\"orderId\":\"%s\",\"userId\":\"%s\",\"productId\":\"%s\",\"quantity\":%d,\"status\":\"CREATED\"}",
                    orderId, userId, productId, quantity
                );
                operations.send(KafkaConstants.TOPIC_ORDER, orderId, orderMessage);
                log.info("【事务生产者-订单处理】订单消息已发送");
                
                // 2. 发送库存扣减消息
                String inventoryMessage = String.format(
                    "{\"productId\":\"%s\",\"quantity\":%d,\"operation\":\"DECREASE\"}",
                    productId, quantity
                );
                operations.send("inventory-topic", productId, inventoryMessage);
                log.info("【事务生产者-订单处理】库存扣减消息已发送");
                
                // 3. 发送用户积分消息
                String pointsMessage = String.format(
                    "{\"userId\":\"%s\",\"points\":%d,\"operation\":\"ADD\"}",
                    userId, quantity * 10
                );
                operations.send("points-topic", userId, pointsMessage);
                log.info("【事务生产者-订单处理】用户积分消息已发送");
                
                // 如果任何一条消息失败，所有消息都会回滚
                return true;
            });
            
            log.info("【事务生产者-订单处理】订单处理完成，事务提交成功");
            
        } catch (Exception e) {
            log.error("【事务生产者-订单处理】订单处理失败，事务回滚", e);
            // 这里可以进行补偿操作
        }
    }
}

