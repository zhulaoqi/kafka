package com.kinch.common.serializer;

import com.alibaba.fastjson2.JSON;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;

/**
 * 通用JSON序列化器 - 使用FastJSON2
 * 用于演示自定义序列化器的实现
 */
public class JsonSerializer<T> implements Serializer<T> {
    
    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) {
            return null;
        }
        
        try {
            // 使用FastJSON2进行序列化
            String jsonStr = JSON.toJSONString(data);
            return jsonStr.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SerializationException("Error serializing JSON message", e);
        }
    }
}

