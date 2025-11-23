# Kafka深度学习项目

这是一个完整的Kafka学习项目，涵盖了Kafka的核心知识点和最佳实践。

## 项目结构

```
kafka-learning/
├── kafka-common/          # 公共模块
│   ├── model/            # 实体类
│   ├── serializer/       # 自定义序列化器
│   ├── constant/         # 常量定义
│   └── util/            # 工具类
├── producer/             # 生产者模块
│   ├── config/          # 配置类
│   ├── partition/       # 自定义分区器
│   ├── interceptor/     # 拦截器
│   ├── service/         # 业务服务
│   └── controller/      # REST API
└── consumer/             # 消费者模块
    ├── config/          # 配置类
    ├── listener/        # 消费监听器
    └── controller/      # REST API
```

## 核心特性展示

### 生产者特性

#### 1. 基础发送方式
- **发后即忘（Fire and Forget）**：最快但可能丢消息
- **同步发送（Synchronous）**：最慢但最可靠
- **异步发送（Asynchronous）**：高性能且可靠（推荐）

#### 2. 分区策略
- **自定义分区器**：`CustomPartitioner` 
  - VIP用户消息发送到特定分区
  - 普通用户使用Hash分区保证负载均衡
- **带Key发送**：相同Key的消息发送到同一分区，保证顺序性
- **指定分区发送**：手动控制消息发送到哪个分区

#### 3. 幂等性生产者
- **配置**：`enable.idempotence=true`
- **特性**：保证在重试情况下消息不重复
- **原理**：Producer为每条消息分配序列号，Broker去重
- **场景**：防止网络抖动导致的重复消息

#### 4. 事务生产者
- **配置**：`transactional.id`
- **特性**：跨分区、跨Topic的原子性保证
- **场景**：
  - 订单处理：同时发送订单消息、库存消息、积分消息
  - 数据一致性：要么全部成功，要么全部失败

#### 5. 拦截器
- **功能**：
  - 发送前添加公共Header（消息序号、时间戳、来源等）
  - 发送后统计成功率和失败率
  - 监控和链路追踪

### 消费者特性

#### 1. 基础消费
- **手动提交偏移量**：保证At Least Once语义
- **Header读取**：获取消息元数据
- **多Topic消费**：一个消费者消费多个Topic
- **指定分区消费**：消费特定分区
- **指定偏移量消费**：从特定位置开始消费

#### 2. 批量消费
- **优势**：提高吞吐量，减少网络开销
- **配置**：`setBatchListener(true)`
- **注意**：批量失败时的处理策略

#### 3. 消费者组
- **负载均衡**：同一组内的消费者共享分区
- **独立消费**：不同组独立消费，互不影响
- **分区分配**：
  - 1个消费者：消费所有分区
  - N个消费者（N≤分区数）：每个消费者消费部分分区
  - N个消费者（N>分区数）：多余的消费者空闲

#### 4. 偏移量管理
- **At Most Once**：先提交后处理（可能丢消息）
- **At Least Once**：先处理后提交（可能重复，推荐）
- **Exactly Once**：事务 + 幂等性（不丢不重）
- **手动提交**：精确控制提交时机
- **批量提交**：提高性能

#### 5. 重平衡（Rebalance）
- **触发条件**：
  - 消费者加入/离开
  - 订阅的Topic变化
  - 分区数量变化
- **影响**：整个消费者组短暂停止消费
- **监听器**：`ConsumerRebalanceListener`
  - `onPartitionsRevoked`：分区撤销前
  - `onPartitionsAssigned`：分区分配后
- **优化**：使用CooperativeStickyAssignor策略

## 快速开始

### 1. 启动Kafka环境

```bash
# 启动Kafka集群（包括Zookeeper、Kafka、Kafka UI）
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f kafka
```

访问Kafka UI：http://localhost:8080

### 2. 编译项目

```bash
# 编译整个项目
mvn clean install

# 或者分别编译
cd kafka-common && mvn clean install
cd ../producer && mvn clean package
cd ../consumer && mvn clean package
```

### 3. 启动生产者

```bash
cd producer
mvn spring-boot:run

# 或使用jar
java -jar target/producer-0.0.1-SNAPSHOT.jar
```

生产者API地址：http://localhost:8081

### 4. 启动消费者

```bash
cd consumer
mvn spring-boot:run

# 或使用jar
java -jar target/consumer-0.0.1-SNAPSHOT.jar
```

消费者API地址：http://localhost:8082

## API测试示例

### 生产者API

```bash
# 1. 健康检查
curl http://localhost:8081/api/producer/health

# 2. 发后即忘
curl -X POST "http://localhost:8081/api/producer/fire-and-forget?message=test"

# 3. 同步发送
curl -X POST "http://localhost:8081/api/producer/sync?message=test"

# 4. 异步发送
curl -X POST "http://localhost:8081/api/producer/async?message=test"

# 5. 带Key发送
curl -X POST "http://localhost:8081/api/producer/with-key?key=user-123&message=test"

# 6. 幂等性发送
curl -X POST "http://localhost:8081/api/producer/idempotent?key=user-123&message=test"

# 7. 幂等性批量发送
curl -X POST "http://localhost:8081/api/producer/idempotent/batch?keyPrefix=user&count=100"

# 8. 事务发送
curl -X POST "http://localhost:8081/api/producer/transaction?message1=msg1&message2=msg2"

# 9. 事务回滚测试
curl -X POST "http://localhost:8081/api/producer/transaction/rollback?message1=msg1&message2=msg2&shouldFail=true"

# 10. 订单处理事务
curl -X POST "http://localhost:8081/api/producer/transaction/order?orderId=001&userId=100&productId=200&quantity=5"
```

### 消费者API

```bash
# 健康检查
curl http://localhost:8082/api/consumer/health

# 获取消费者信息
curl http://localhost:8082/api/consumer/info
```

## 核心配置说明

### 生产者核心配置

```properties
# ACK配置
acks=all                    # all: 等待所有ISR确认（最可靠）
                           # 1: 只等待Leader确认
                           # 0: 不等待确认（最快）

# 重试配置
retries=3                   # 重试次数
retry.backoff.ms=100       # 重试间隔

# 批量发送
batch.size=16384           # 批量大小（16KB）
linger.ms=10               # 等待时间（10ms）

# 压缩
compression.type=snappy    # snappy/gzip/lz4/zstd

# 幂等性
enable.idempotence=true    # 开启幂等性

# 事务
transactional.id=xxx       # 事务ID
```

### 消费者核心配置

```properties
# 消费者组
group.id=xxx               # 消费者组ID

# 偏移量
auto.offset.reset=earliest # earliest/latest/none
enable.auto.commit=false   # 建议手动提交

# 拉取配置
max.poll.records=100       # 单次拉取最大消息数
max.poll.interval.ms=300000 # 处理超时时间（5分钟）

# 心跳配置
session.timeout.ms=10000   # 会话超时（10秒）
heartbeat.interval.ms=3000 # 心跳间隔（3秒）

# 事务隔离
isolation.level=read_committed # 只读已提交的事务消息
```

## 学习路径建议

### 第一阶段：基础概念
1. 理解Producer、Consumer、Broker、Topic、Partition的概念
2. 运行基础的发送和接收代码
3. 通过Kafka UI查看消息

### 第二阶段：核心特性
1. 学习分区策略和消息顺序性
2. 掌握偏移量管理和提交策略
3. 理解消费者组和负载均衡
4. 实践重平衡监听

### 第三阶段：高级特性
1. 掌握幂等性生产者的使用场景
2. 理解事务的原理和应用
3. 学习拦截器的使用
4. 实现死信队列和重试机制

### 第四阶段：性能优化
1. 批量发送和批量消费
2. 压缩配置
3. 分区数量和消费者数量的平衡
4. 避免频繁重平衡

### 第五阶段：生产实践
1. 监控和告警
2. 数据一致性保证
3. 故障恢复
4. 性能调优

## 常见问题

### 1. 消息丢失怎么办？
- 生产者：使用`acks=all`，开启重试
- Broker：增加副本数，配置`min.insync.replicas`
- 消费者：手动提交偏移量，先处理后提交

### 2. 消息重复怎么办？
- 生产者：开启幂等性
- 消费者：实现幂等性业务逻辑（如使用唯一ID去重）
- 使用事务实现精确一次语义

### 3. 消息顺序怎么保证？
- 同一个Key的消息会发送到同一个分区
- 单个分区内消息有序
- 消费者顺序处理同一分区的消息

### 4. 重平衡太频繁怎么办？
- 增加`max.poll.interval.ms`
- 减少`max.poll.records`
- 优化业务处理逻辑，减少处理时间
- 使用协作式重平衡策略

### 5. 如何提高吞吐量？
- 生产者：批量发送、压缩、增加缓冲区
- Broker：增加分区数、调整副本数
- 消费者：批量消费、增加消费者数量

## 技术栈

- Java 21
- Spring Boot 3.3.5
- Spring Kafka 3.3.0
- Apache Kafka 3.9.0（对应Confluent Platform 7.9.1）
- FastJSON2 2.0.53
- Lombok 1.18.34
- Docker & Docker Compose

## 参考资料

- [Apache Kafka官方文档](https://kafka.apache.org/documentation/)
- [Confluent文档](https://docs.confluent.io/)
- [Spring Kafka文档](https://docs.spring.io/spring-kafka/reference/)

## 作者

Kinch

## 许可证

MIT License

