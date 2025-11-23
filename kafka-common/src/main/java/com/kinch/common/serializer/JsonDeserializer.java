package com.kinch.common.serializer;

import com.alibaba.fastjson2.JSON;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 通用JSON反序列化器 - 使用FastJSON2
 * 用于演示自定义反序列化器的实现
 */
public class JsonDeserializer<T> implements Deserializer<T> {
    
    private Class<T> targetType;
    
    public JsonDeserializer() {
    }
    
    public JsonDeserializer(Class<T> targetType) {
        this.targetType = targetType;
    }
    
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        if (targetType == null) {
            // 尝试从配置中获取目标类型
            Object typeConfig = configs.get("value.deserializer.target.type");
            if (typeConfig instanceof Class) {
                this.targetType = (Class<T>) typeConfig;
            } else if (typeConfig instanceof String) {
                try {
                    this.targetType = (Class<T>) Class.forName((String) typeConfig);
                } catch (ClassNotFoundException e) {
                    throw new SerializationException("Cannot find target type class: " + typeConfig, e);
                }
            }
        }
    }
    
    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        
        if (targetType == null) {
            throw new SerializationException("Target type is not configured for deserialization");
        }
        
        try {
            String jsonStr = new String(data, StandardCharsets.UTF_8);
            return JSON.parseObject(jsonStr, targetType);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing JSON message", e);
        }
    }
}

