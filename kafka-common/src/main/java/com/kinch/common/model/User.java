package com.kinch.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类 - 用于演示自定义对象的序列化
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String username;
    private String email;
    private Integer age;
    private LocalDateTime createTime;
    
    /**
     * 业务方法 - 用于演示
     */
    public String getDisplayName() {
        return username + " (" + email + ")";
    }
}

