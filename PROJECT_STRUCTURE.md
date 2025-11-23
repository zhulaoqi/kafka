# 项目结构详解

## 整体架构

```
kafka-learning/
│
├── kafka-common/                    # 📦 公共模块
│   └── src/main/java/com/kinch/common/
│       ├── model/                   # 实体类
│       │   ├── User.java           # 用户实体
│       │   └── Order.java          # 订单实体
│       ├── serializer/             # 序列化器
│       │   ├── JsonSerializer.java      # JSON序列化
│       │   └── JsonDeserializer.java    # JSON反序列化
│       ├── constant/               # 常量
│       │   └── KafkaConstants.java # Kafka常量定义
│       └── util/                   # 工具类
│           └── KafkaUtils.java     # Kafka工具方法
│
├── producer/                        # 🚀 生产者模块
│   └── src/main/java/com/kinch/producer/
│       ├── ProducerApplication.java         # 启动类
│       ├── config/                          # 配置
│       │   └── KafkaProducerConfig.java    # 生产者配置
│       ├── partition/                       # 分区
│       │   └── CustomPartitioner.java      # 自定义分区器
│       ├── interceptor/                     # 拦截器
│       │   └── ProducerInterceptorExample.java # 拦截器示例
│       ├── service/                         # 服务
│       │   ├── BasicProducerService.java       # 基础生产者
│       │   ├── IdempotentProducerService.java  # 幂等性生产者
│       │   └── TransactionalProducerService.java # 事务生产者
│       └── controller/                      # 控制器
│           └── ProducerController.java     # REST API
│
├── consumer/                        # 📥 消费者模块
│   └── src/main/java/com/kinch/consumer/
│       ├── ConsumerApplication.java         # 启动类
│       ├── config/                          # 配置
│       │   └── KafkaConsumerConfig.java    # 消费者配置
│       ├── listener/                        # 监听器
│       │   ├── BasicConsumerListener.java      # 基础消费
│       │   ├── BatchConsumerListener.java      # 批量消费
│       │   ├── ConsumerGroupListener.java      # 消费者组
│       │   ├── OffsetManagementListener.java   # 偏移量管理
│       │   └── RebalanceListener.java          # 重平衡监听
│       └── controller/                      # 控制器
│           └── ConsumerController.java     # REST API
│
├── scripts/                         # 🔧 脚本
│   ├── start-all.sh                # 一键启动
│   ├── stop-all.sh                 # 一键停止
│   └── test-scenarios.sh           # 测试场景
│
├── docker-compose.yml              # 🐳 Docker配置
├── pom.xml                         # 📋 Maven父配置
├── README.md                       # 📖 项目说明
├── QUICKSTART.md                   # ⚡ 快速开始
├── LEARNING_NOTES.md               # 📝 学习笔记
└── PROJECT_STRUCTURE.md            # 📁 本文件
```

## 核心功能模块

### 1. 公共模块（kafka-common）

#### 1.1 实体类（model）
- **User.java**: 用户实体，演示自定义对象序列化
- **Order.java**: 订单实体，演示事务消息

#### 1.2 序列化器（serializer）
- **JsonSerializer**: 使用FastJSON2序列化对象
- **JsonDeserializer**: 使用FastJSON2反序列化对象

#### 1.3 常量（constant）
- **KafkaConstants**: 定义Topic名称、消费者组ID、Header常量等

#### 1.4 工具类（util）
- **KafkaUtils**: Topic管理、消息ID生成等工具方法

### 2. 生产者模块（producer）

#### 2.1 配置类（config）
**KafkaProducerConfig.java**
- `stringProducerFactory()`: 基础生产者配置
- `jsonProducerFactory()`: JSON生产者配置
- `idempotentProducerFactory()`: 幂等性生产者配置
- `transactionalProducerFactory()`: 事务生产者配置

#### 2.2 分区器（partition）
**CustomPartitioner.java**
- VIP用户消息路由到特定分区
- 普通用户使用Hash分区

#### 2.3 拦截器（interceptor）
**ProducerInterceptorExample.java**
- 发送前添加Header（消息序号、时间戳、来源）
- 发送后统计成功率和失败率

#### 2.4 服务类（service）

**BasicProducerService.java** - 基础发送
- `sendFireAndForget()`: 发后即忘
- `sendSync()`: 同步发送
- `sendAsync()`: 异步发送
- `sendWithKey()`: 带Key发送（保证顺序）
- `sendToPartition()`: 指定分区发送
- `sendWithHeaders()`: 带Header发送

**IdempotentProducerService.java** - 幂等性
- `sendIdempotentMessage()`: 幂等性发送
- `sendBatch()`: 批量发送
- `sendWithRetryScenario()`: 重试场景演示

**TransactionalProducerService.java** - 事务
- `sendInTransaction()`: 基础事务
- `sendCrossTopics()`: 跨Topic事务
- `sendWithRollback()`: 事务回滚演示
- `sendBatchInTransaction()`: 批量事务
- `processOrderInTransaction()`: 订单处理事务

#### 2.5 控制器（controller）
**ProducerController.java** - 提供REST API测试接口

### 3. 消费者模块（consumer）

#### 3.1 配置类（config）
**KafkaConsumerConfig.java**
- `stringConsumerFactory()`: 基础消费者配置
- `kafkaListenerContainerFactory()`: 单条消费容器
- `batchKafkaListenerContainerFactory()`: 批量消费容器
- `transactionalConsumerFactory()`: 事务消费者配置
- `jsonConsumerFactory()`: JSON消费者配置

#### 3.2 监听器（listener）

**BasicConsumerListener.java** - 基础消费
- 基础消费（手动提交）
- 消费带Header的消息
- 多Topic消费
- 指定分区消费
- 指定偏移量消费

**BatchConsumerListener.java** - 批量消费
- 批量消费消息
- 批量处理失败策略
- 死信队列处理

**ConsumerGroupListener.java** - 消费者组
- 消费者组1（3个并发线程）
- 消费者组2（独立消费）
- 负载均衡演示

**OffsetManagementListener.java** - 偏移量管理
- 手动提交偏移量
- 批量提交偏移量
- 指定偏移量消费
- 不同提交时机演示

**RebalanceListener.java** - 重平衡
- `onPartitionsRevoked()`: 分区撤销前
- `onPartitionsAssigned()`: 分区分配后
- `onPartitionsLost()`: 分区丢失时

#### 3.3 控制器（controller）
**ConsumerController.java** - 健康检查和监控接口

## 代码知识点映射

### 生产者核心特性

| 特性 | 代码位置 | 配置项 |
|------|---------|--------|
| ACK机制 | KafkaProducerConfig | `acks=all` |
| 批量发送 | KafkaProducerConfig | `batch.size=16384`<br>`linger.ms=10` |
| 压缩 | KafkaProducerConfig | `compression.type=snappy` |
| 幂等性 | IdempotentProducerService | `enable.idempotence=true` |
| 事务 | TransactionalProducerService | `transactional.id=xxx` |
| 自定义分区 | CustomPartitioner | 实现`Partitioner`接口 |
| 拦截器 | ProducerInterceptorExample | 实现`ProducerInterceptor`接口 |

### 消费者核心特性

| 特性 | 代码位置 | 配置项 |
|------|---------|--------|
| 手动提交 | BasicConsumerListener | `enable.auto.commit=false` |
| 批量消费 | BatchConsumerListener | `setBatchListener(true)` |
| 消费者组 | ConsumerGroupListener | `group.id=xxx` |
| 偏移量管理 | OffsetManagementListener | `ack.acknowledge()` |
| 重平衡监听 | RebalanceListener | 实现`ConsumerRebalanceListener` |
| 并发消费 | KafkaConsumerConfig | `concurrency=3` |

## 学习路径

### 第一阶段：基础理解
1. 查看 `BasicProducerService.java` 和 `BasicConsumerListener.java`
2. 理解Producer、Consumer、Topic、Partition的概念
3. 运行基础的发送和接收测试

### 第二阶段：核心特性
1. 学习 `CustomPartitioner.java` - 理解分区策略
2. 学习 `OffsetManagementListener.java` - 理解偏移量管理
3. 学习 `ConsumerGroupListener.java` - 理解消费者组
4. 学习 `RebalanceListener.java` - 理解重平衡

### 第三阶段：高级特性
1. 学习 `IdempotentProducerService.java` - 理解幂等性
2. 学习 `TransactionalProducerService.java` - 理解事务
3. 学习 `ProducerInterceptorExample.java` - 理解拦截器
4. 学习 `BatchConsumerListener.java` - 理解批量消费

### 第四阶段：实战应用
1. 实现死信队列
2. 实现消息重试机制
3. 实现业务幂等性
4. 性能调优

## 配置文件说明

### producer/application.properties
```properties
# 服务端口
server.port=8081

# Kafka服务器
spring.kafka.bootstrap-servers=localhost:9092

# ACK配置
spring.kafka.producer.acks=all

# 批量发送
spring.kafka.producer.batch-size=16384
spring.kafka.producer.linger-ms=10

# 压缩
spring.kafka.producer.compression-type=snappy
```

### consumer/application.properties
```properties
# 服务端口
server.port=8082

# Kafka服务器
spring.kafka.bootstrap-servers=localhost:9092

# 消费者组
spring.kafka.consumer.group-id=default-consumer-group

# 偏移量重置
spring.kafka.consumer.auto-offset-reset=earliest

# 手动提交
spring.kafka.consumer.enable-auto-commit=false

# 并发数
spring.kafka.listener.concurrency=3
```

## Docker环境

### docker-compose.yml
提供以下服务：
- **Zookeeper**: 端口2181
- **Kafka**: 端口9092（外部）、9093（内部）
- **Kafka UI**: 端口8080

### 启动命令
```bash
# 启动所有服务
./scripts/start-all.sh

# 停止所有服务
./scripts/stop-all.sh

# 测试场景
./scripts/test-scenarios.sh
```

## API接口

### 生产者API (http://localhost:8081)

#### 基础发送
- `POST /api/producer/fire-and-forget?message=xxx`
- `POST /api/producer/sync?message=xxx`
- `POST /api/producer/async?message=xxx`
- `POST /api/producer/with-key?key=xxx&message=xxx`
- `POST /api/producer/to-partition?partition=0&message=xxx`
- `POST /api/producer/with-headers?key=xxx&message=xxx`

#### 幂等性
- `POST /api/producer/idempotent?key=xxx&message=xxx`
- `POST /api/producer/idempotent/batch?keyPrefix=xxx&count=100`

#### 事务
- `POST /api/producer/transaction?message1=xxx&message2=xxx`
- `POST /api/producer/transaction/cross-topics?orderMsg=xxx&userMsg=xxx`
- `POST /api/producer/transaction/rollback?message1=xxx&message2=xxx&shouldFail=true`
- `POST /api/producer/transaction/batch?keyPrefix=xxx&count=50`
- `POST /api/producer/transaction/order?orderId=xxx&userId=xxx&productId=xxx&quantity=5`

### 消费者API (http://localhost:8082)
- `GET /api/consumer/health` - 健康检查
- `GET /api/consumer/info` - 消费者信息

## 监控和调试

### Kafka UI (http://localhost:8080)
- 查看Topics和消息
- 查看Consumer Groups和Lag
- 查看Broker状态

### 日志查看
```bash
# 生产者日志
tail -f logs/producer.log

# 消费者日志
tail -f logs/consumer.log

# Kafka日志
docker-compose logs -f kafka
```

## 总结

这个项目涵盖了Kafka的核心知识点：
- ✅ 生产者：ACK、批量、压缩、幂等性、事务、分区、拦截器
- ✅ 消费者：消费者组、偏移量管理、重平衡、批量消费
- ✅ 可靠性：消息不丢失、不重复、保证顺序
- ✅ 性能：批量处理、压缩、并发消费

通过学习和运行这个项目，你将深入理解Kafka的工作原理和最佳实践！

