package com.kinch.producer.controller;

import com.kinch.producer.service.BasicProducerService;
import com.kinch.producer.service.IdempotentProducerService;
import com.kinch.producer.service.TransactionalProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 生产者REST API控制器
 * 提供HTTP接口方便测试各种生产者功能
 */
@Slf4j
@RestController
@RequestMapping("/api/producer")
public class ProducerController {
    
    @Autowired
    private BasicProducerService basicProducerService;
    
    @Autowired
    private IdempotentProducerService idempotentProducerService;
    
    @Autowired
    private TransactionalProducerService transactionalProducerService;
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "kafka-producer");
        return result;
    }
    
    // ==================== 基础生产者 ====================
    
    /**
     * 发后即忘
     * 示例: POST /api/producer/fire-and-forget?message=test
     */
    @PostMapping("/fire-and-forget")
    public Map<String, Object> sendFireAndForget(@RequestParam String message) {
        basicProducerService.sendFireAndForget(message);
        return successResponse("发后即忘发送成功");
    }
    
    /**
     * 同步发送
     * 示例: POST /api/producer/sync?message=test
     */
    @PostMapping("/sync")
    public Map<String, Object> sendSync(@RequestParam String message) {
        basicProducerService.sendSync(message);
        return successResponse("同步发送成功");
    }
    
    /**
     * 异步发送
     * 示例: POST /api/producer/async?message=test
     */
    @PostMapping("/async")
    public Map<String, Object> sendAsync(@RequestParam String message) {
        basicProducerService.sendAsync(message);
        return successResponse("异步发送已提交");
    }
    
    /**
     * 带Key发送
     * 示例: POST /api/producer/with-key?key=user-123&message=test
     */
    @PostMapping("/with-key")
    public Map<String, Object> sendWithKey(
            @RequestParam String key,
            @RequestParam String message) {
        basicProducerService.sendWithKey(key, message);
        return successResponse("带Key发送成功");
    }
    
    /**
     * 指定分区发送
     * 示例: POST /api/producer/to-partition?partition=0&message=test
     */
    @PostMapping("/to-partition")
    public Map<String, Object> sendToPartition(
            @RequestParam int partition,
            @RequestParam String message) {
        basicProducerService.sendToPartition(partition, message);
        return successResponse("指定分区发送成功");
    }
    
    /**
     * 带Header发送
     * 示例: POST /api/producer/with-headers?key=user-123&message=test
     */
    @PostMapping("/with-headers")
    public Map<String, Object> sendWithHeaders(
            @RequestParam String key,
            @RequestParam String message) {
        basicProducerService.sendWithHeaders(key, message);
        return successResponse("带Header发送成功");
    }
    
    // ==================== 幂等性生产者 ====================
    
    /**
     * 幂等性发送
     * 示例: POST /api/producer/idempotent?key=user-123&message=test
     */
    @PostMapping("/idempotent")
    public Map<String, Object> sendIdempotent(
            @RequestParam String key,
            @RequestParam String message) {
        idempotentProducerService.sendIdempotentMessage(key, message);
        return successResponse("幂等性发送成功");
    }
    
    /**
     * 幂等性批量发送
     * 示例: POST /api/producer/idempotent/batch?keyPrefix=user&count=100
     */
    @PostMapping("/idempotent/batch")
    public Map<String, Object> sendIdempotentBatch(
            @RequestParam String keyPrefix,
            @RequestParam(defaultValue = "10") int count) {
        idempotentProducerService.sendBatch(keyPrefix, count);
        return successResponse("幂等性批量发送已提交，数量：" + count);
    }
    
    // ==================== 事务生产者 ====================
    
    /**
     * 事务发送
     * 示例: POST /api/producer/transaction?message1=test1&message2=test2
     */
    @PostMapping("/transaction")
    public Map<String, Object> sendInTransaction(
            @RequestParam String message1,
            @RequestParam String message2) {
        transactionalProducerService.sendInTransaction(message1, message2);
        return successResponse("事务发送成功");
    }
    
    /**
     * 跨Topic事务发送
     * 示例: POST /api/producer/transaction/cross-topics?orderMsg=order1&userMsg=user1
     */
    @PostMapping("/transaction/cross-topics")
    public Map<String, Object> sendCrossTopics(
            @RequestParam String orderMsg,
            @RequestParam String userMsg) {
        transactionalProducerService.sendCrossTopics(orderMsg, userMsg);
        return successResponse("跨Topic事务发送成功");
    }
    
    /**
     * 事务回滚测试
     * 示例: POST /api/producer/transaction/rollback?message1=test1&message2=test2&shouldFail=true
     */
    @PostMapping("/transaction/rollback")
    public Map<String, Object> sendWithRollback(
            @RequestParam String message1,
            @RequestParam String message2,
            @RequestParam(defaultValue = "false") boolean shouldFail) {
        try {
            transactionalProducerService.sendWithRollback(message1, message2, shouldFail);
            return successResponse("事务发送完成（是否回滚：" + shouldFail + "）");
        } catch (Exception e) {
            return errorResponse("事务发送失败：" + e.getMessage());
        }
    }
    
    /**
     * 事务批量发送
     * 示例: POST /api/producer/transaction/batch?keyPrefix=tx&count=50
     */
    @PostMapping("/transaction/batch")
    public Map<String, Object> sendTransactionBatch(
            @RequestParam String keyPrefix,
            @RequestParam(defaultValue = "10") int count) {
        transactionalProducerService.sendBatchInTransaction(keyPrefix, count);
        return successResponse("事务批量发送成功，数量：" + count);
    }
    
    /**
     * 订单处理事务
     * 示例: POST /api/producer/transaction/order?orderId=001&userId=100&productId=200&quantity=5
     */
    @PostMapping("/transaction/order")
    public Map<String, Object> processOrder(
            @RequestParam String orderId,
            @RequestParam String userId,
            @RequestParam String productId,
            @RequestParam int quantity) {
        transactionalProducerService.processOrderInTransaction(orderId, userId, productId, quantity);
        return successResponse("订单处理完成");
    }
    
    // ==================== 工具方法 ====================
    
    private Map<String, Object> successResponse(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", message);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
    
    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}

