package com.kinch.common.constant;

/**
 * Kafka常量类
 */
public class KafkaConstants {
    
    // ==================== Topic名称 ====================
    public static final String TOPIC_SIMPLE = "simple-topic";
    public static final String TOPIC_USER = "user-topic";
    public static final String TOPIC_ORDER = "order-topic";
    public static final String TOPIC_TRANSACTION = "transaction-topic";
    public static final String TOPIC_PARTITION_TEST = "partition-test-topic";
    public static final String TOPIC_DLQ = "dead-letter-queue"; // 死信队列
    
    // ==================== 消费者组 ====================
    public static final String GROUP_SIMPLE = "simple-consumer-group";
    public static final String GROUP_USER = "user-consumer-group";
    public static final String GROUP_ORDER = "order-consumer-group";
    public static final String GROUP_MULTIPLE_1 = "multiple-consumer-group-1";
    public static final String GROUP_MULTIPLE_2 = "multiple-consumer-group-2";
    
    // ==================== 消息头 ====================
    public static final String HEADER_MESSAGE_ID = "message-id";
    public static final String HEADER_TIMESTAMP = "timestamp";
    public static final String HEADER_SOURCE = "source";
    public static final String HEADER_TRACE_ID = "trace-id";
    
    // ==================== 分区数量 ====================
    public static final int PARTITION_COUNT_DEFAULT = 3;
    public static final int PARTITION_COUNT_HIGH = 6;
    
    // ==================== 副本数量 ====================
    public static final short REPLICATION_FACTOR = 1;
    
    private KafkaConstants() {
        // 工具类，防止实例化
    }
}

