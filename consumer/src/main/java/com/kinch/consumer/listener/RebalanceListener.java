package com.kinch.consumer.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * 重平衡监听器
 * 
 * 核心知识点：
 * 1. 重平衡（Rebalance）：重新分配分区给消费者的过程
 * 2. 触发条件：
 *    - 消费者加入/离开消费者组
 *    - 订阅的Topic分区数量变化
 *    - 消费者崩溃（心跳超时）
 * 3. 影响：
 *    - 重平衡期间，整个消费者组停止消费（Stop-The-World）
 *    - 可能导致消息重复消费
 * 4. 优化策略：
 *    - 合理设置session.timeout.ms（心跳超时时间）
 *    - 合理设置max.poll.interval.ms（处理超时时间）
 *    - 使用CooperativeStickyAssignor分区分配策略
 *    - 避免长时间处理单条消息
 * 
 * 分区分配策略：
 * 1. RangeAssignor（默认）：按范围分配
 * 2. RoundRobinAssignor：轮询分配
 * 3. StickyAssignor：粘性分配，尽量保持原分配
 * 4. CooperativeStickyAssignor：协作式粘性分配，减少停顿时间（推荐）
 */
@Slf4j
@Component
public class RebalanceListener implements ConsumerRebalanceListener {
    
    /**
     * 分区被撤销前调用
     * 
     * 使用场景：
     * 1. 保存当前消费进度
     * 2. 清理资源
     * 3. 提交未提交的偏移量
     * 
     * 重要：这个方法必须快速执行完成，否则会延长重平衡时间
     */
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        log.warn("【重平衡-撤销】开始撤销分区，数量: {}", partitions.size());
        
        for (TopicPartition partition : partitions) {
            log.warn("【重平衡-撤销】分区: {}-{}", partition.topic(), partition.partition());
        }
        
        // 重要操作：
        // 1. 提交当前偏移量，避免消息重复
        // commitCurrentOffsets();
        
        // 2. 保存处理状态
        // saveProcessingState(partitions);
        
        // 3. 清理资源
        // cleanupResources(partitions);
        
        log.warn("【重平衡-撤销】完成撤销分区");
    }
    
    /**
     * 分区分配后调用
     * 
     * 使用场景：
     * 1. 初始化资源
     * 2. 加载之前的处理状态
     * 3. 记录分区分配信息
     */
    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        log.info("【重平衡-分配】开始分配分区，数量: {}", partitions.size());
        
        for (TopicPartition partition : partitions) {
            log.info("【重平衡-分配】分区: {}-{}", partition.topic(), partition.partition());
        }
        
        // 重要操作：
        // 1. 初始化分区相关资源
        // initializeResources(partitions);
        
        // 2. 加载处理状态
        // loadProcessingState(partitions);
        
        // 3. 记录分配时间，用于监控
        // recordAssignmentTime(partitions);
        
        log.info("【重平衡-分配】完成分配分区");
    }
    
    /**
     * 分区丢失时调用（仅在eager rebalance协议中）
     * 在cooperative rebalance中不会调用
     */
    @Override
    public void onPartitionsLost(Collection<TopicPartition> partitions) {
        log.error("【重平衡-丢失】分区丢失，数量: {}", partitions.size());
        
        for (TopicPartition partition : partitions) {
            log.error("【重平衡-丢失】分区: {}-{}", partition.topic(), partition.partition());
        }
        
        // 分区丢失时的处理：
        // 1. 记录异常
        // 2. 清理资源
        // 3. 发送告警
        
        log.error("【重平衡-丢失】完成处理丢失分区");
    }
    
    /**
     * 演示：避免不必要的重平衡
     * 
     * 常见导致重平衡的问题：
     * 1. 处理时间过长，超过max.poll.interval.ms
     * 2. GC暂停导致心跳超时
     * 3. 网络抖动
     * 
     * 解决方案：
     * 1. 增加max.poll.interval.ms
     * 2. 减少max.poll.records
     * 3. 优化业务处理逻辑
     * 4. 使用协作式重平衡策略
     */
    public void demonstrateRebalanceOptimization(Consumer<?, ?> consumer) {
        log.info("【重平衡优化】演示重平衡优化技巧");
        
        // 技巧1：处理前暂停消费，避免超时
        // consumer.pause(consumer.assignment());
        // processLongRunningTask();
        // consumer.resume(consumer.assignment());
        
        // 技巧2：定期发送心跳（新版本自动处理）
        // consumer.enforceRebalance(); // 主动触发重平衡
        
        // 技巧3：监控重平衡频率
        // 如果重平衡频繁，需要检查配置和业务逻辑
    }
    
    /**
     * 提交当前偏移量
     */
    private void commitCurrentOffsets() {
        log.debug("【重平衡-撤销】提交当前偏移量");
        // 实际应该使用Consumer.commitSync()
    }
    
    /**
     * 保存处理状态
     */
    private void saveProcessingState(Collection<TopicPartition> partitions) {
        log.debug("【重平衡-撤销】保存处理状态");
        // 保存到Redis、数据库等
    }
    
    /**
     * 清理资源
     */
    private void cleanupResources(Collection<TopicPartition> partitions) {
        log.debug("【重平衡-撤销】清理资源");
        // 关闭连接、释放内存等
    }
    
    /**
     * 初始化资源
     */
    private void initializeResources(Collection<TopicPartition> partitions) {
        log.debug("【重平衡-分配】初始化资源");
        // 创建连接、分配内存等
    }
    
    /**
     * 加载处理状态
     */
    private void loadProcessingState(Collection<TopicPartition> partitions) {
        log.debug("【重平衡-分配】加载处理状态");
        // 从Redis、数据库等加载
    }
}

