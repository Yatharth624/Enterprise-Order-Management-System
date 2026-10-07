package com.enterprise.oms.payment.service;

import com.enterprise.oms.common.api.ApiResponse;
import com.enterprise.oms.payment.domain.IdempotencyRecord;
import com.enterprise.oms.payment.repository.IdempotencyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    public <T> ApiResponse<T> checkIdempotency(String key, Class<T> dataType) {
        if (key == null || key.isBlank()) {
            return null;
        }

        // 1. Check Redis cache
        if (redisTemplate != null) {
            try {
                String cachedJson = redisTemplate.opsForValue().get("idempotency:" + key);
                if (cachedJson != null) {
                    log.info("Idempotency cache hit in Redis for key: {}", key);
                    return (ApiResponse<T>) objectMapper.readValue(cachedJson, objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, dataType));
                }
            } catch (Exception e) {
                log.warn("Redis error checking idempotency key {}: {}", key, e.getMessage());
            }
        }

        // 2. Check PostgreSQL database
        return idempotencyRepository.findByIdempotencyKey(key).map(record -> {
            try {
                log.info("Idempotency hit in DB for key: {}", key);
                return (ApiResponse<T>) objectMapper.readValue(record.getResponseBody(), objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, dataType));
            } catch (Exception e) {
                log.error("Failed to parse cached response for idempotency key {}", key, e);
                return null;
            }
        }).orElse(null);
    }

    @Transactional
    public void saveIdempotencyRecord(String key, Object responsePayload) {
        if (key == null || key.isBlank()) return;

        try {
            String jsonPayload = objectMapper.writeValueAsString(responsePayload);

            IdempotencyRecord record = IdempotencyRecord.builder()
                    .idempotencyKey(key)
                    .responseBody(jsonPayload)
                    .build();
            idempotencyRepository.save(record);

            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set("idempotency:" + key, jsonPayload, Duration.ofHours(24));
                } catch (Exception e) {
                    log.warn("Redis error saving idempotency key {}: {}", key, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to save idempotency record for key {}", key, e);
        }
    }
}
