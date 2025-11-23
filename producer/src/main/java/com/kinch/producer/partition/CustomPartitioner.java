package com.kinch.producer.partition;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 自定义分区器
 * 演示如何控制消息发送到哪个分区
 * 
 * 核心知识点：
 * 1. 分区策略影响消息的顺序性和负载均衡
 * 2. 有Key时可以保证相同Key的消息发送到同一分区
 * 3. 无Key时默认使用轮询策略
 */
public class CustomPartitioner implements Partitioner {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomPartitioner.class);
    
    @Override
    public void configure(Map<String, ?> configs) {
        // 初始化配置，可以从这里读取自定义配置
        logger.info("自定义分区器初始化，配置: {}", configs);
    }
    
    /**
     * 核心方法：决定消息发送到哪个分区
     * 
     * @param topic 主题名称
     * @param key 消息key
     * @param keyBytes key的字节数组
     * @param value 消息value
     * @param valueBytes value的字节数组
     * @param cluster 集群元数据
     * @return 目标分区编号
     */
    @Override
    public int partition(String topic, Object key, byte[] keyBytes, 
                        Object value, byte[] valueBytes, Cluster cluster) {
        
        List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
        int numPartitions = partitions.size();
        
        if (keyBytes == null) {
            // 没有key时，使用轮询策略
            // 这里简化处理，实际生产环境可以使用更复杂的策略
            return (int) (System.currentTimeMillis() % numPartitions);
        }
        
        // 自定义分区策略1：根据key的特定前缀分区
        String keyStr = new String(keyBytes);
        
        // 例如：VIP用户发送到特定分区
        if (keyStr.startsWith("VIP-")) {
            // VIP用户发送到0号分区（假设这是一个高性能分区）
            logger.info("VIP用户消息，发送到分区0: key={}", keyStr);
            return 0;
        }
        
        // 例如：普通用户按照hash分区
        // 使用Kafka自带的murmur2算法保证均匀分布
        int partition = Utils.toPositive(Utils.murmur2(keyBytes)) % numPartitions;
        
        logger.debug("消息分区计算: key={}, partition={}/{}", keyStr, partition, numPartitions);
        return partition;
    }
    
    @Override
    public void close() {
        // 清理资源
        logger.info("自定义分区器关闭");
    }
}

