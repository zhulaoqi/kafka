package com.kinch.consumer.listener;

import com.kinch.common.constant.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 消费者组监听器
 * 
 * 核心知识点：
 * 1. 消费者组（Consumer Group）是Kafka实现负载均衡和故障转移的关键
 * 2. 同一消费者组内的消费者共享消费进度（偏移量）
 * 3. 每个分区只能被同一消费者组内的一个消费者消费
 * 4. 不同消费者组之间互不影响，可以独立消费所有消息
 * 5. 消费者数量 > 分区数量时，多余的消费者会空闲
 * 
 * 重平衡（Rebalance）：
 * 1. 触发条件：消费者加入/离开、订阅的Topic变化、分区数量变化
 * 2. 影响：重平衡期间，消费者组无法消费消息（短暂停顿）
 * 3. 优化：避免频繁重平衡，合理设置session.timeout.ms和max.poll.interval.ms
 */
@Slf4j
@Component
public class ConsumerGroupListener {
    
    /**
     * 消费者组1 - 实例1
     * 说明：启动多个实例时，它们会自动负载均衡
     */
    @KafkaListener(
        topics = KafkaConstants.TOPIC_PARTITION_TEST,
        groupId = KafkaConstants.GROUP_MULTIPLE_1,
        containerFactory = "kafkaListenerContainerFactory",
        concurrency = "3" // 3个并发线程，相当于3个消费者
    )
    public void consumeGroup1(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            log.info("【消费者组1】收到消息 - Thread: {}, Partition: {}, Offset: {}, Key: {}, Value: {}", 
                Thread.currentThread().getName(),
                record.partition(),
                record.offset(),
                record.key(),
                record.value());
            
            // 模拟处理耗时
            Thread.sleep(100);
            
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("【消费者组1】消息处理失败", e);
        }
    }
    
    /**
     * 消费者组2 - 独立消费
     * 说明：与消费者组1消费相同的Topic，但进度独立
     * 这样可以实现：
     * 1. 一份数据，多个系统消费
     * 2. 实时处理 + 离线分析
     */
    @KafkaListener(
        topics = KafkaConstants.TOPIC_PARTITION_TEST,
        groupId = KafkaConstants.GROUP_MULTIPLE_2,
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeGroup2(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            log.info("【消费者组2】收到消息 - Partition: {}, Offset: {}, Key: {}", 
                record.partition(),
                record.offset(),
                record.key());
            
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("【消费者组2】消息处理失败", e);
        }
    }
    
    /**
     * 演示消费者组的负载均衡
     * 
     * 场景说明：
     * - 假设Topic有3个分区：P0, P1, P2
     * - 启动1个消费者：该消费者消费所有3个分区
     * - 启动2个消费者：分区分配可能是 [P0,P1] 和 [P2]
     * - 启动3个消费者：分区分配可能是 [P0], [P1], [P2]
     * - 启动4个消费者：分区分配可能是 [P0], [P1], [P2], []（最后一个空闲）
     * 
     * 分区分配策略：
     * 1. RangeAssignor：按范围分配（默认）
     * 2. RoundRobinAssignor：轮询分配
     * 3. StickyAssignor：粘性分配，尽量保持原有分配
     * 4. CooperativeStickyAssignor：协作式粘性分配（推荐）
     */
    @KafkaListener(
        topics = KafkaConstants.TOPIC_PARTITION_TEST,
        groupId = "load-balance-demo-group",
        containerFactory = "kafkaListenerContainerFactory",
        concurrency = "2" // 2个线程
    )
    public void consumeLoadBalance(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            log.info("【负载均衡演示】Thread: {}, Partition: {}, Offset: {}", 
                Thread.currentThread().getName(),
                record.partition(),
                record.offset());
            
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("【负载均衡演示】消息处理失败", e);
        }
    }
}

