package com.enterprise.oms.common.lock;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@NoArgsConstructor
public class DistributedLockService {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private final Map<String, ReentrantLock> localLocks = new ConcurrentHashMap<>();

    public DistributedLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean acquireLock(String lockKey, String lockValue, long ttlSeconds) {
        if (redisTemplate != null) {
            try {
                Boolean success = redisTemplate.opsForValue()
                        .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(ttlSeconds));
                return Boolean.TRUE.equals(success);
            } catch (Exception e) {
                log.warn("Redis unavailable for lock acquiring, falling back to in-memory lock: {}", e.getMessage());
            }
        }
        
        ReentrantLock lock = localLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        try {
            return lock.tryLock(0, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void releaseLock(String lockKey, String lockValue) {
        if (redisTemplate != null) {
            try {
                String currentValue = redisTemplate.opsForValue().get(lockKey);
                if (lockValue.equals(currentValue)) {
                    redisTemplate.delete(lockKey);
                }
                return;
            } catch (Exception e) {
                log.warn("Redis error releasing lock: {}", e.getMessage());
            }
        }

        ReentrantLock lock = localLocks.get(lockKey);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
