#!/bin/bash

# Kafka学习项目 - 一键启动脚本

echo "=========================================="
echo "  Kafka学习项目 - 启动脚本"
echo "=========================================="
echo ""

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ 错误：Docker未运行，请先启动Docker"
    exit 1
fi

# 进入项目根目录
cd "$(dirname "$0")/.." || exit

echo "📦 第一步：编译项目..."
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "❌ 编译失败"
    exit 1
fi
echo "✅ 编译完成"
echo ""

echo "🐳 第二步：启动Kafka集群..."
docker-compose up -d
if [ $? -ne 0 ]; then
    echo "❌ Kafka启动失败"
    exit 1
fi
echo "✅ Kafka集群启动成功"
echo ""

echo "⏳ 等待Kafka就绪（30秒）..."
sleep 30

echo "🚀 第三步：启动生产者..."
cd producer || exit
nohup mvn spring-boot:run > ../logs/producer.log 2>&1 &
PRODUCER_PID=$!
echo "✅ 生产者启动中，PID: $PRODUCER_PID"
echo ""

echo "🚀 第四步：启动消费者..."
cd ../consumer || exit
nohup mvn spring-boot:run > ../logs/consumer.log 2>&1 &
CONSUMER_PID=$!
echo "✅ 消费者启动中，PID: $CONSUMER_PID"
echo ""

echo "⏳ 等待应用启动（20秒）..."
sleep 20

echo ""
echo "=========================================="
echo "  ✅ 所有服务启动完成！"
echo "=========================================="
echo ""
echo "📊 服务地址："
echo "  - Kafka UI:  http://localhost:8080"
echo "  - 生产者API: http://localhost:8081"
echo "  - 消费者API: http://localhost:8082"
echo ""
echo "📝 测试命令："
echo "  curl -X POST \"http://localhost:8081/api/producer/async?message=Hello-Kafka\""
echo ""
echo "📋 查看日志："
echo "  tail -f logs/producer.log"
echo "  tail -f logs/consumer.log"
echo "  docker-compose logs -f kafka"
echo ""
echo "🛑 停止服务："
echo "  ./scripts/stop-all.sh"
echo ""

