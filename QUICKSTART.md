# 快速开始指南

本指南帮助你快速启动Kafka学习项目并进行测试。

## 前置要求

- Java 21
- Maven 3.6+
- Docker & Docker Compose
- curl（用于测试API）

## 第一步：启动Kafka环境

```bash
# 1. 进入项目目录
cd kafka-learning

# 2. 启动Kafka集群
docker-compose up -d

# 3. 等待服务启动（约30秒）
docker-compose ps

# 4. 查看Kafka日志，确认启动成功
docker-compose logs kafka | grep "started"
```

### 验证Kafka环境

访问Kafka UI：http://localhost:8080

你应该能看到：
- Brokers: 1个
- Topics: 可能还没有（会在第一次发送消息时自动创建）

## 第二步：编译项目

```bash
# 编译整个项目
mvn clean install -DskipTests

# 查看编译结果
ls -l kafka-common/target/*.jar
ls -l producer/target/*.jar
ls -l consumer/target/*.jar
```

## 第三步：启动应用

### 启动生产者（终端1）

```bash
cd producer
mvn spring-boot:run
```

等待看到：`Started ProducerApplication`

### 启动消费者（终端2）

```bash
cd consumer
mvn spring-boot:run
```

等待看到：`Started ConsumerApplication`

## 第四步：测试核心特性

### 测试1：基础发送（终端3）

```bash
# 异步发送消息
curl -X POST "http://localhost:8081/api/producer/async?message=Hello-Kafka"

# 查看消费者日志，应该能看到消息被消费
```

### 测试2：带Key发送（保证顺序）

```bash
# 发送多条带相同Key的消息
for i in {1..5}; do
  curl -X POST "http://localhost:8081/api/producer/with-key?key=user-123&message=msg-$i"
  sleep 0.5
done

# 观察消费者日志，这些消息会被发送到同一个分区，保证顺序
```

### 测试3：幂等性（防重复）

```bash
# 批量发送100条消息
curl -X POST "http://localhost:8081/api/producer/idempotent/batch?keyPrefix=test&count=100"

# 观察生产者日志中的统计信息
# 即使网络重试，也不会产生重复消息
```

### 测试4：事务（原子性）

```bash
# 发送事务消息（成功）
curl -X POST "http://localhost:8081/api/producer/transaction?message1=事务消息1&message2=事务消息2"

# 发送事务消息（失败回滚）
curl -X POST "http://localhost:8081/api/producer/transaction/rollback?message1=msg1&message2=msg2&shouldFail=true"

# 观察消费者日志：
# - 第一个请求的两条消息都会被消费
# - 第二个请求的两条消息都不会被消费（事务回滚）
```

### 测试5：订单处理事务

```bash
# 模拟订单处理
curl -X POST "http://localhost:8081/api/producer/transaction/order?orderId=ORD001&userId=U001&productId=P001&quantity=5"

# 观察生产者日志，会发送3条消息到不同Topic：
# - order-topic: 订单消息
# - inventory-topic: 库存扣减消息
# - points-topic: 用户积分消息
# 这3条消息要么全部成功，要么全部失败
```

## 第五步：在Kafka UI中观察

访问 http://localhost:8080

### 查看Topics
1. 点击左侧 "Topics"
2. 你应该能看到：
   - simple-topic
   - user-topic
   - order-topic
   - partition-test-topic
   等

### 查看消息
1. 点击某个Topic
2. 点击 "Messages" 标签
3. 可以看到：
   - 消息内容
   - Key
   - Partition
   - Offset
   - Headers（如果有）

### 查看消费者组
1. 点击左侧 "Consumers"
2. 你应该能看到：
   - simple-consumer-group
   - batch-consumer-group
   - manual-offset-group
   等
3. 查看每个组的Lag（消费延迟）

## 测试场景演示

### 场景1：消费者负载均衡

```bash
# 1. 发送100条消息到partition-test-topic
for i in {1..100}; do
  curl -X POST "http://localhost:8081/api/producer/async?message=test-$i" &
done

# 2. 观察消费者日志，你会看到：
#    - 消息被分配到不同的分区
#    - 不同的消费者线程处理不同的分区
#    - 这就是消费者的负载均衡
```

### 场景2：消息顺序性

```bash
# 1. 发送10条带相同Key的消息
for i in {1..10}; do
  curl -X POST "http://localhost:8081/api/producer/with-key?key=order-123&message=step-$i"
  sleep 0.2
done

# 2. 观察消费者日志：
#    - 所有消息都发送到同一个分区
#    - 消费顺序与发送顺序一致
#    - 这保证了同一个订单的操作按顺序处理
```

### 场景3：批量消费

```bash
# 1. 快速发送大量消息
for i in {1..200}; do
  curl -X POST "http://localhost:8081/api/producer/async?message=batch-$i" &
done

# 2. 观察batch-consumer-group的日志：
#    - 消息会被批量消费
#    - 每次处理多条消息
#    - 吞吐量更高
```

## 常见问题排查

### 1. 无法连接Kafka

```bash
# 检查Docker容器状态
docker-compose ps

# 查看Kafka日志
docker-compose logs kafka

# 重启Kafka
docker-compose restart kafka
```

### 2. 消费者没有收到消息

```bash
# 检查消费者是否启动
curl http://localhost:8082/api/consumer/health

# 查看消费者日志
# 检查是否有错误信息

# 在Kafka UI中检查消费者组的Lag
# 如果Lag为0，说明消息已被消费
```

### 3. Topic没有自动创建

```bash
# 手动创建Topic
docker exec -it kafka-broker kafka-topics \
  --create \
  --bootstrap-server localhost:9093 \
  --topic simple-topic \
  --partitions 3 \
  --replication-factor 1

# 列出所有Topic
docker exec -it kafka-broker kafka-topics \
  --list \
  --bootstrap-server localhost:9093
```

### 4. 查看Topic详情

```bash
# 查看Topic配置
docker exec -it kafka-broker kafka-topics \
  --describe \
  --bootstrap-server localhost:9093 \
  --topic simple-topic

# 查看消费者组详情
docker exec -it kafka-broker kafka-consumer-groups \
  --describe \
  --bootstrap-server localhost:9093 \
  --group simple-consumer-group
```

## 学习建议

### 第一天：基础测试
1. 完成上面的测试1-5
2. 在Kafka UI中观察消息流转
3. 理解Producer、Consumer、Topic、Partition的概念

### 第二天：深入特性
1. 阅读代码中的注释，理解原理
2. 修改配置，观察行为变化
3. 测试不同的发送和消费方式

### 第三天：实践应用
1. 尝试实现自己的业务场景
2. 处理异常情况
3. 优化性能

## 下一步

- 阅读 [README.md](README.md) 了解详细的特性说明
- 查看代码中的注释，理解实现原理
- 尝试修改配置，观察行为变化
- 实现自己的业务场景

## 停止服务

```bash
# 停止应用（Ctrl+C）

# 停止Kafka
docker-compose down

# 清理数据（慎用！会删除所有消息）
docker-compose down -v
```

## 获取帮助

如果遇到问题，请：
1. 查看日志文件
2. 检查Kafka UI中的状态
3. 参考README.md中的常见问题
4. 查看官方文档

