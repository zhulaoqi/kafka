package com.kinch.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类 - 用于演示事务消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String orderId;
    private Long userId;
    private String productName;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String status; // PENDING, CONFIRMED, CANCELLED
    private LocalDateTime orderTime;
    
    public void confirm() {
        this.status = "CONFIRMED";
    }
    
    public void cancel() {
        this.status = "CANCELLED";
    }
}

