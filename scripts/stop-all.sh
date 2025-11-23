#!/bin/bash

# Kafka学习项目 - 一键停止脚本

echo "=========================================="
echo "  Kafka学习项目 - 停止脚本"
echo "=========================================="
echo ""

# 进入项目根目录
cd "$(dirname "$0")/.." || exit

echo "🛑 停止Spring Boot应用..."
# 查找并停止Spring Boot应用
pkill -f "spring-boot:run" || true
pkill -f "producer.*\.jar" || true
pkill -f "consumer.*\.jar" || true
echo "✅ Spring Boot应用已停止"
echo ""

echo "🛑 停止Kafka集群..."
docker-compose down
echo "✅ Kafka集群已停止"
echo ""

echo "=========================================="
echo "  ✅ 所有服务已停止"
echo "=========================================="
echo ""
echo "💡 提示："
echo "  - 如需清理数据：docker-compose down -v"
echo "  - 重新启动：./scripts/start-all.sh"
echo ""

