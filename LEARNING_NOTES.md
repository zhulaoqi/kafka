# Kafka核心知识点学习笔记

这份笔记整理了Kafka的核心知识点，帮助你深入理解本项目中的代码。

## 目录
1. [Kafka架构](#kafka架构)
2. [生产者核心知识](#生产者核心知识)
3. [消费者核心知识](#消费者核心知识)
4. [性能优化](#性能优化)
5. [最佳实践](#最佳实践)

---

## Kafka架构

### 核心组件

```
┌─────────────────────────────────────────────────┐
│                   Producer                       │
│              (生产者发送消息)                      │
└────────────────┬────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────┐
│                  Kafka Cluster                   │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐   │
│  │  Broker 1 │  │  Broker 2 │  │  Broker 3 │   │
│  │           │  │           │  │           │   │
│  │  Topic A  │  │  Topic A  │  │  Topic A  │   │
│  │  P0  P1   │  │  P2  P3   │  │  P4  P5   │   │
│  └───────────┘  └───────────┘  └───────────┘   │
└────────────────┬────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────┐
│              Consumer Group                      │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │Consumer 1│  │Consumer 2│  │Consumer 3│      │
│  │  P0 P1   │  │  P2 P3   │  │  P4 P5   │      │
│  └──────────┘  └──────────┘  └──────────┘      │
└─────────────────────────────────────────────────┘
```

### 关键概念

#### 1. Topic（主题）
- **定义**：消息的分类，类似数据库中的表
- **特点**：逻辑概念，由多个分区组成
- **命名**：建议使用有意义的名称，如`user-events`、`order-created`

#### 2. Partition（分区）
- **定义**：Topic的物理分片，消息的有序队列
- **作用**：
  - 实现并行处理
  - 提高吞吐量
  - 保证分区内消息有序
- **分配**：消息通过Key的Hash值或自定义分区器分配到分区

#### 3. Offset（偏移量）
- **定义**：消息在分区中的唯一序号
- **特点**：
  - 单调递增
  - 每个分区独立维护
  - 消费者通过Offset知道消费到哪里

#### 4. Replication（副本）
- **定义**：分区的备份
- **类型**：
  - Leader副本：处理读写请求
  - Follower副本：同步Leader的数据
- **作用**：提高可靠性和容错能力

---

## 生产者核心知识

### 1. 发送流程

```
Producer发送流程：
1. Serializer    → 序列化Key和Value
2. Partitioner   → 确定目标分区
3. Accumulator   → 消息累加器（批处理）
4. Sender        → 网络发送线程
5. Broker        → Kafka服务器接收
6. Callback      → 回调处理结果
```

### 2. ACK机制

| ACK值 | 含义 | 可靠性 | 性能 | 使用场景 |
|-------|------|--------|------|----------|
| 0 | 不等待确认 | 低（可能丢消息） | 高 | 日志收集 |
| 1 | 等待Leader确认 | 中（Leader宕机可能丢） | 中 | 一般场景 |
| all/-1 | 等待所有ISR确认 | 高 | 低 | 重要数据 |

**代码示例**：
```java
// KafkaProducerConfig.java
configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // 最高可靠性
```

### 3. 幂等性生产者

**问题**：网络抖动导致重试，可能产生重复消息

**解决**：开启幂等性
```java
// 配置
configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
```

**原理**：
1. Producer为每条消息分配唯一的`<PID, Epoch, Sequence Number>`
2. Broker记录每个Producer的最大序列号
3. 重复的消息（相同序列号）会被去重

**限制**：
- 单个Producer
- 单个分区
- 单次会话（重启后失效）

**代码位置**：`IdempotentProducerService.java`

### 4. 事务生产者

**场景**：需要保证多条消息的原子性

**配置**：
```java
configProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "tx-producer-");
configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
```

**使用**：
```java
kafkaTemplate.executeInTransaction(operations -> {
    operations.send(topic1, message1); // 操作1
    operations.send(topic2, message2); // 操作2
    // 要么都成功，要么都失败
    return true;
});
```

**原理**：两阶段提交
1. **Prepare阶段**：Producer向Transaction Coordinator发起事务
2. **Commit阶段**：所有消息发送成功后提交，失败则回滚

**代码位置**：`TransactionalProducerService.java`

### 5. 分区策略

#### 默认分区策略
```java
if (key == null) {
    // 粘性分区：同一批次发到同一分区
    return stickyPartition;
} else {
    // Hash分区：相同Key发到同一分区
    return hash(key) % numPartitions;
}
```

#### 自定义分区器
```java
public class CustomPartitioner implements Partitioner {
    @Override
    public int partition(String topic, Object key, byte[] keyBytes, 
                        Object value, byte[] valueBytes, Cluster cluster) {
        // 自定义逻辑：VIP用户发到特定分区
        if (key.toString().startsWith("VIP-")) {
            return 0;
        }
        return hash(keyBytes) % numPartitions;
    }
}
```

**代码位置**：`CustomPartitioner.java`

### 6. 拦截器

**作用**：在发送前后进行统一处理

**应用场景**：
- 添加公共Header（TraceId、时间戳）
- 统计发送成功率
- 监控和告警

```java
public class ProducerInterceptorExample implements ProducerInterceptor<String, String> {
    @Override
    public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
        // 发送前：添加Header
        record.headers().add("trace-id", traceId.getBytes());
        return record;
    }
    
    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        // 发送后：统计
        if (exception == null) {
            successCount.incrementAndGet();
        } else {
            failureCount.incrementAndGet();
        }
    }
}
```

**代码位置**：`ProducerInterceptorExample.java`

---

## 消费者核心知识

### 1. 消费者组（Consumer Group）

**核心概念**：
- 同一组内的消费者共享消费进度
- 每个分区只能被组内一个消费者消费
- 不同组之间独立消费

**分区分配示例**：
```
Topic: 6个分区 [P0, P1, P2, P3, P4, P5]

场景1：1个消费者
Consumer1 → [P0, P1, P2, P3, P4, P5]

场景2：2个消费者
Consumer1 → [P0, P1, P2]
Consumer2 → [P3, P4, P5]

场景3：3个消费者
Consumer1 → [P0, P1]
Consumer2 → [P2, P3]
Consumer3 → [P4, P5]

场景4：7个消费者（超过分区数）
Consumer1 → [P0]
Consumer2 → [P1]
...
Consumer6 → [P5]
Consumer7 → []  (空闲)
```

**代码位置**：`ConsumerGroupListener.java`

### 2. 偏移量管理

#### 提交策略对比

| 策略 | 提交时机 | 消息语义 | 优缺点 |
|------|----------|----------|--------|
| 自动提交 | 定时提交 | At Most Once | 简单但可能丢消息 |
| 手动同步提交 | 处理后立即提交 | At Least Once | 可靠但性能低 |
| 手动异步提交 | 处理后异步提交 | At Least Once | 性能好但可能重复 |
| 批量提交 | 处理N条后提交 | At Least Once | 高性能但重复多 |

#### At Least Once（推荐）
```java
@KafkaListener(topics = "my-topic")
public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
    try {
        // 1. 先处理消息
        processMessage(record);
        
        // 2. 后提交偏移量
        ack.acknowledge();
        
        // 如果处理失败，不提交偏移量，下次重新消费
    } catch (Exception e) {
        // 不提交，下次重新消费（可能重复，但不丢失）
        log.error("处理失败", e);
    }
}
```

#### At Most Once（不推荐）
```java
// 先提交，后处理（可能丢消息）
ack.acknowledge();  // 先提交
processMessage(record);  // 后处理
```

#### Exactly Once
```java
// 方案1：使用事务
kafkaTemplate.executeInTransaction(operations -> {
    // 消费 + 处理 + 生产 在一个事务中
});

// 方案2：实现幂等性业务逻辑
public void processMessage(Record record) {
    String messageId = record.header("message-id");
    if (isAlreadyProcessed(messageId)) {
        return; // 已处理过，跳过
    }
    // 处理消息 + 保存messageId（原子操作）
}
```

**代码位置**：`OffsetManagementListener.java`

### 3. 重平衡（Rebalance）

#### 触发条件
1. 消费者加入或离开
2. 订阅的Topic变化
3. 分区数量变化
4. 消费者崩溃（心跳超时）

#### 重平衡流程
```
1. 停止消费                (Stop-The-World)
2. 撤销分区                (onPartitionsRevoked)
3. 重新分配分区             (Coordinator决策)
4. 分配分区                (onPartitionsAssigned)
5. 恢复消费
```

#### 影响
- 重平衡期间，整个消费者组停止消费
- 可能导致消息重复消费
- 频繁重平衡会严重影响性能

#### 优化策略
```java
// 1. 增加处理超时时间
max.poll.interval.ms=300000  // 5分钟

// 2. 减少单次拉取数量
max.poll.records=100

// 3. 合理设置心跳
session.timeout.ms=10000      // 10秒
heartbeat.interval.ms=3000    // 3秒

// 4. 使用协作式重平衡（推荐）
partition.assignment.strategy=CooperativeStickyAssignor
```

#### 重平衡监听器
```java
public class RebalanceListener implements ConsumerRebalanceListener {
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        // 分区撤销前：提交偏移量、保存状态、清理资源
        log.warn("分区被撤销: {}", partitions);
        commitCurrentOffsets();
    }
    
    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        // 分区分配后：初始化资源、加载状态
        log.info("分区被分配: {}", partitions);
        initializeResources(partitions);
    }
}
```

**代码位置**：`RebalanceListener.java`

### 4. 批量消费

**优势**：
- 提高吞吐量（减少网络往返）
- 适合批量入库场景

**配置**：
```java
factory.setBatchListener(true);
```

**使用**：
```java
@KafkaListener(topics = "my-topic", containerFactory = "batchKafkaListenerContainerFactory")
public void consumeBatch(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
    // 批量处理
    batchProcess(records);
    
    // 批量提交
    ack.acknowledge();
}
```

**注意**：批量失败时的处理
```java
try {
    batchProcess(records);
    ack.acknowledge();
} catch (Exception e) {
    // 策略1：不提交，下次重新消费整批
    // 策略2：转为单条处理，找出失败的消息
    for (ConsumerRecord<String, String> record : records) {
        try {
            processSingle(record);
        } catch (Exception ex) {
            sendToDeadLetterQueue(record);
        }
    }
}
```

**代码位置**：`BatchConsumerListener.java`

---

## 性能优化

### 1. 生产者优化

#### 批量发送
```java
// 批量大小（默认16KB）
batch.size=16384

// 等待时间（默认0ms）
linger.ms=10  // 等待10ms收集更多消息
```

**效果**：10ms延迟换取更高的吞吐量

#### 压缩
```java
compression.type=snappy  // snappy/gzip/lz4/zstd
```

**对比**：
| 压缩算法 | 压缩率 | CPU消耗 | 速度 | 推荐场景 |
|---------|--------|---------|------|----------|
| none | - | 低 | 最快 | 低延迟 |
| snappy | 中 | 低 | 快 | 通用（推荐） |
| lz4 | 中 | 低 | 快 | 高吞吐 |
| gzip | 高 | 高 | 慢 | 带宽受限 |
| zstd | 高 | 中 | 中 | 新版本推荐 |

#### 缓冲区
```java
buffer.memory=33554432  // 32MB
```

### 2. 消费者优化

#### 并发消费
```java
// Spring Kafka配置
factory.setConcurrency(3);  // 3个线程并发消费
```

#### 拉取配置
```java
max.poll.records=500  // 单次拉取更多消息
fetch.min.bytes=1     // 至少拉取1字节
fetch.max.wait.ms=500 // 最多等待500ms
```

### 3. Broker优化

#### 分区数量
```
分区数 ≈ 目标吞吐量 / 单分区吞吐量
```

**建议**：
- 初始分区数：3-6个
- 可以增加但不能减少
- 过多分区会增加端到端延迟

#### 副本数量
```
副本数 = 1 + 容错数量
```

**建议**：
- 开发环境：1个副本
- 生产环境：3个副本（容忍2个Broker故障）

---

## 最佳实践

### 1. 消息不丢失

#### 生产者端
```java
// 1. ACK设置为all
acks=all

// 2. 开启重试
retries=3

// 3. 开启幂等性
enable.idempotence=true

// 4. 使用回调确认
kafkaTemplate.send(topic, message).whenComplete((result, ex) -> {
    if (ex != null) {
        // 记录失败日志，人工介入
        saveToFailureLog(message);
    }
});
```

#### Broker端
```java
// 1. 增加副本数
replication.factor=3

// 2. 设置最小同步副本数
min.insync.replicas=2

// 3. 禁止自动创建Topic（避免误操作）
auto.create.topics.enable=false
```

#### 消费者端
```java
// 1. 手动提交偏移量
enable.auto.commit=false

// 2. 先处理后提交
processMessage(record);
ack.acknowledge();

// 3. 实现重试机制
int retryCount = 0;
while (retryCount < MAX_RETRY) {
    try {
        processMessage(record);
        break;
    } catch (Exception e) {
        retryCount++;
        if (retryCount >= MAX_RETRY) {
            sendToDeadLetterQueue(record);
        }
    }
}
```

### 2. 消息不重复

#### 生产者端
```java
// 开启幂等性
enable.idempotence=true

// 或使用事务
transactional.id=my-tx-id
```

#### 消费者端（业务幂等）
```java
// 方案1：数据库唯一索引
INSERT INTO orders (order_id, ...) VALUES (?, ...)
// order_id设置为唯一索引，重复插入会失败

// 方案2：分布式锁
if (redisLock.tryLock(messageId)) {
    try {
        processMessage(record);
    } finally {
        redisLock.unlock(messageId);
    }
}

// 方案3：消息去重表
if (!isProcessed(messageId)) {
    processMessage(record);
    markAsProcessed(messageId);
}
```

### 3. 消息顺序

#### 保证顺序的方法
```java
// 方法1：使用Key
// 相同Key的消息会发送到同一分区
kafkaTemplate.send(topic, orderId, message);

// 方法2：指定分区
int partition = calculatePartition(orderId);
ProducerRecord<String, String> record = 
    new ProducerRecord<>(topic, partition, null, message);
kafkaTemplate.send(record);

// 方法3：单分区（不推荐，性能差）
```

#### 注意事项
- 只能保证分区内有序
- 重试可能打乱顺序（解决：`max.in.flight.requests.per.connection=1`）
- 重平衡可能影响顺序

### 4. 监控指标

#### 生产者监控
```
- 发送速率（records/sec）
- 发送失败率
- 平均延迟
- 缓冲区使用率
```

#### 消费者监控
```
- 消费速率（records/sec）
- 消费Lag（未消费消息数）
- 重平衡次数
- 提交失败次数
```

#### Broker监控
```
- CPU、内存、磁盘使用率
- 网络吞吐量
- 分区数、副本数
- ISR缩小/扩大次数
```

### 5. 故障处理

#### 死信队列（DLQ）
```java
try {
    processMessage(record);
} catch (Exception e) {
    if (retryCount >= MAX_RETRY) {
        // 发送到死信队列
        kafkaTemplate.send(DLQ_TOPIC, record.key(), record.value());
        // 记录失败原因
        saveFail ureLog(record, e);
    }
}
```

#### 重试策略
```java
// 1. 立即重试（适合瞬时错误）
// 2. 延迟重试（适合依赖服务暂时不可用）
// 3. 指数退避重试（1s, 2s, 4s, 8s, ...）
```

---

## 总结

### 核心知识点清单

✅ **生产者**
- [ ] 理解ACK机制（0, 1, all）
- [ ] 掌握幂等性配置和原理
- [ ] 掌握事务使用场景
- [ ] 理解分区策略
- [ ] 会使用拦截器

✅ **消费者**
- [ ] 理解消费者组和负载均衡
- [ ] 掌握偏移量管理（手动提交）
- [ ] 理解三种消息语义
- [ ] 掌握重平衡机制
- [ ] 会使用批量消费

✅ **可靠性**
- [ ] 知道如何保证消息不丢失
- [ ] 知道如何保证消息不重复
- [ ] 知道如何保证消息顺序

✅ **性能**
- [ ] 掌握批量发送和批量消费
- [ ] 了解压缩配置
- [ ] 理解分区数和消费者数的关系

### 学习建议

1. **动手实践**：运行本项目的所有示例代码
2. **查看日志**：观察消息流转过程
3. **修改配置**：体验不同配置的效果
4. **制造故障**：模拟网络中断、消费者宕机等场景
5. **性能测试**：测试吞吐量和延迟
6. **阅读源码**：深入理解Kafka原理

### 推荐阅读

- 《Kafka权威指南》
- 《深入理解Kafka》
- [Kafka官方文档](https://kafka.apache.org/documentation/)
- [Confluent文档](https://docs.confluent.io/)

---

**祝学习愉快！🎉**

