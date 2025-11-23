package com.kinch.consumer.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 消费者REST API控制器
 * 提供健康检查和监控接口
 */
@Slf4j
@RestController
@RequestMapping("/api/consumer")
public class ConsumerController {
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "kafka-consumer");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
    
    /**
     * 获取消费者信息
     */
    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> result = new HashMap<>();
        result.put("name", "Kafka Consumer Learning Project");
        result.put("description", "展示Kafka消费者核心特性");
        result.put("features", new String[]{
            "基础消费",
            "批量消费",
            "消费者组",
            "偏移量管理",
            "重平衡监听",
            "事务消费"
        });
        return result;
    }
}

