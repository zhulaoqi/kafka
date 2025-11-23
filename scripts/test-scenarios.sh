#!/bin/bash

# Kafka学习项目 - 测试场景脚本

BASE_URL="http://localhost:8081/api/producer"

echo "=========================================="
echo "  Kafka核心特性测试场景"
echo "=========================================="
echo ""

# 检查服务是否可用
check_service() {
    if ! curl -s "${BASE_URL%/*}/producer/health" > /dev/null 2>&1; then
        echo "❌ 错误：生产者服务未运行"
        echo "请先运行: ./scripts/start-all.sh"
        exit 1
    fi
}

# 场景1：基础发送
test_basic() {
    echo "📝 场景1：基础发送方式"
    echo "----------------------------------------"
    
    echo "1. 发后即忘..."
    curl -X POST "${BASE_URL}/fire-and-forget?message=测试消息-发后即忘"
    echo ""
    
    echo "2. 同步发送..."
    curl -X POST "${BASE_URL}/sync?message=测试消息-同步发送"
    echo ""
    
    echo "3. 异步发送..."
    curl -X POST "${BASE_URL}/async?message=测试消息-异步发送"
    echo ""
    
    echo "✅ 场景1完成"
    echo ""
}

# 场景2：消息顺序性
test_order() {
    echo "📝 场景2：消息顺序性（相同Key）"
    echo "----------------------------------------"
    
    for i in {1..5}; do
        echo "发送消息 $i/5..."
        curl -X POST "${BASE_URL}/with-key?key=order-123&message=订单操作-步骤$i"
        echo ""
        sleep 0.5
    done
    
    echo "✅ 场景2完成 - 查看消费者日志，验证消息顺序"
    echo ""
}

# 场景3：幂等性
test_idempotent() {
    echo "📝 场景3：幂等性（防重复）"
    echo "----------------------------------------"
    
    echo "批量发送100条消息..."
    curl -X POST "${BASE_URL}/idempotent/batch?keyPrefix=test&count=100"
    echo ""
    
    echo "✅ 场景3完成 - 查看生产者日志中的统计信息"
    echo ""
}

# 场景4：事务
test_transaction() {
    echo "📝 场景4：事务（原子性）"
    echo "----------------------------------------"
    
    echo "1. 事务成功场景..."
    curl -X POST "${BASE_URL}/transaction?message1=事务消息1&message2=事务消息2"
    echo ""
    
    sleep 1
    
    echo "2. 事务回滚场景..."
    curl -X POST "${BASE_URL}/transaction/rollback?message1=msg1&message2=msg2&shouldFail=true"
    echo ""
    
    echo "✅ 场景4完成 - 查看消费者日志，验证事务效果"
    echo ""
}

# 场景5：订单处理
test_order_processing() {
    echo "📝 场景5：订单处理（多Topic事务）"
    echo "----------------------------------------"
    
    echo "创建订单..."
    curl -X POST "${BASE_URL}/transaction/order?orderId=ORD001&userId=U001&productId=P001&quantity=5"
    echo ""
    
    echo "✅ 场景5完成 - 查看Kafka UI中的3个Topic"
    echo ""
}

# 场景6：性能测试
test_performance() {
    echo "📝 场景6：性能测试（并发发送）"
    echo "----------------------------------------"
    
    echo "并发发送200条消息..."
    for i in {1..200}; do
        curl -X POST "${BASE_URL}/async?message=perf-test-$i" &
    done
    wait
    echo ""
    
    echo "✅ 场景6完成 - 查看消费者吞吐量统计"
    echo ""
}

# 主菜单
show_menu() {
    echo ""
    echo "请选择测试场景："
    echo "  1. 基础发送方式"
    echo "  2. 消息顺序性"
    echo "  3. 幂等性测试"
    echo "  4. 事务测试"
    echo "  5. 订单处理"
    echo "  6. 性能测试"
    echo "  7. 运行全部场景"
    echo "  0. 退出"
    echo ""
    read -p "请输入选项 [0-7]: " choice
}

# 主程序
main() {
    check_service
    
    if [ $# -eq 1 ]; then
        # 命令行参数
        case $1 in
            1) test_basic ;;
            2) test_order ;;
            3) test_idempotent ;;
            4) test_transaction ;;
            5) test_order_processing ;;
            6) test_performance ;;
            7) 
                test_basic
                test_order
                test_idempotent
                test_transaction
                test_order_processing
                test_performance
                ;;
            *) echo "无效选项" ;;
        esac
    else
        # 交互式菜单
        while true; do
            show_menu
            case $choice in
                1) test_basic ;;
                2) test_order ;;
                3) test_idempotent ;;
                4) test_transaction ;;
                5) test_order_processing ;;
                6) test_performance ;;
                7) 
                    test_basic
                    test_order
                    test_idempotent
                    test_transaction
                    test_order_processing
                    test_performance
                    ;;
                0) 
                    echo "退出测试"
                    break
                    ;;
                *) echo "无效选项，请重新选择" ;;
            esac
        done
    fi
}

main "$@"

