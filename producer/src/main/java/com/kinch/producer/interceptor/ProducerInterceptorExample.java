package com.kinch.producer.interceptor;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 生产者拦截器
 * 
 * 核心知识点：
 * 1. 拦截器可以在消息发送前后进行处理
 * 2. 可以用于统计、监控、添加公共信息等
 * 3. 拦截器链：可以配置多个拦截器按顺序执行
 * 4. onSend在序列化之前执行，可以修改消息
 * 5. onAcknowledgement在收到响应后执行，不能修改消息
 */
public class ProducerInterceptorExample implements ProducerInterceptor<String, String> {
    
    private static final Logger logger = LoggerFactory.getLogger(ProducerInterceptorExample.class);
    
    // 统计发送消息数
    private final AtomicLong sendCount = new AtomicLong(0);
    // 统计成功消息数
    private final AtomicLong successCount = new AtomicLong(0);
    // 统计失败消息数
    private final AtomicLong failureCount = new AtomicLong(0);
    
    @Override
    public void configure(Map<String, ?> configs) {
        logger.info("生产者拦截器初始化");
    }
    
    /**
     * 消息发送前调用
     * 可以对消息进行修改或添加header
     */
    @Override
    public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
        long count = sendCount.incrementAndGet();
        
        // 添加自定义header - 消息序号
        ProducerRecord<String, String> newRecord = new ProducerRecord<>(
            record.topic(),
            record.partition(),
            record.timestamp(),
            record.key(),
            record.value(),
            record.headers()
        );
        
        // 添加消息序号
        newRecord.headers().add("message-seq", 
            String.valueOf(count).getBytes(StandardCharsets.UTF_8));
        
        // 添加时间戳
        newRecord.headers().add("send-timestamp", 
            String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
        
        // 添加来源标识
        newRecord.headers().add("source", 
            "producer-interceptor".getBytes(StandardCharsets.UTF_8));
        
        logger.debug("拦截器-发送前: topic={}, key={}, seq={}", 
            record.topic(), record.key(), count);
        
        return newRecord;
    }
    
    /**
     * 收到服务器响应或发送失败时调用
     * 用于统计和监控
     */
    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        if (exception == null) {
            // 发送成功
            long success = successCount.incrementAndGet();
            logger.debug("拦截器-发送成功: topic={}, partition={}, offset={}, 成功数={}", 
                metadata.topic(), metadata.partition(), metadata.offset(), success);
        } else {
            // 发送失败
            long failure = failureCount.incrementAndGet();
            logger.error("拦截器-发送失败: 失败数={}, 错误: {}", 
                failure, exception.getMessage());
        }
        
        // 每100条消息打印一次统计信息
        long total = sendCount.get();
        if (total > 0 && total % 100 == 0) {
            logger.info("【消息统计】总数: {}, 成功: {}, 失败: {}, 成功率: {}", 
                total, successCount.get(), failureCount.get(), 
                String.format("%.2f%%", successCount.get() * 100.0 / total));
        }
    }
    
    @Override
    public void close() {
        // 打印最终统计
        logger.info("【最终统计】总数: {}, 成功: {}, 失败: {}", 
            sendCount.get(), successCount.get(), failureCount.get());
    }
}

