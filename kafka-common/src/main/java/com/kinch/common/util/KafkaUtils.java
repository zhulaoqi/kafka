package com.kinch.common.util;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Kafka工具类
 */
public class KafkaUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaUtils.class);
    
    /**
     * 创建Topic（如果不存在）
     */
    public static void createTopicIfNotExists(String bootstrapServers, 
                                             String topicName, 
                                             int numPartitions, 
                                             short replicationFactor) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        
        try (AdminClient adminClient = AdminClient.create(props)) {
            // 检查Topic是否存在
            Set<String> existingTopics = adminClient.listTopics().names().get();
            
            if (!existingTopics.contains(topicName)) {
                NewTopic newTopic = new NewTopic(topicName, numPartitions, replicationFactor);
                adminClient.createTopics(Collections.singleton(newTopic)).all().get();
                logger.info("Topic创建成功: {}, 分区数: {}, 副本数: {}", 
                    topicName, numPartitions, replicationFactor);
            } else {
                logger.info("Topic已存在: {}", topicName);
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("创建Topic失败: {}", topicName, e);
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 获取Topic的分区数
     */
    public static int getTopicPartitionCount(String bootstrapServers, String topicName) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        
        try (AdminClient adminClient = AdminClient.create(props)) {
            var description = adminClient.describeTopics(Collections.singleton(topicName))
                .allTopicNames().get();
            return description.get(topicName).partitions().size();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("获取Topic分区数失败: {}", topicName, e);
            Thread.currentThread().interrupt();
            return -1;
        }
    }
    
    /**
     * 生成唯一消息ID
     */
    public static String generateMessageId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * 生成TraceID用于链路追踪
     */
    public static String generateTraceId() {
        return System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

