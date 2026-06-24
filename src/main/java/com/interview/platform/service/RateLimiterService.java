package com.interview.platform.service;


import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiterService {

    private final ProxyManager<String> bucketProxyManager;

    @Value("${app.rate-limit.register.capacity:5}")
    private int capacity;

    @Value("${app.rate-limit.register.refill-hours:1}")
    private int refillHours;

    private Supplier<BucketConfiguration> bucketConfiguration() {
        return () -> BucketConfiguration.builder()
            .addLimit(Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, Duration.ofHours(refillHours))
                .build())
            .build();
    }

    public boolean tryConsume(String ipAddress) {
        try {
            String key = "rate_limit:register:" + ipAddress;
            return bucketProxyManager
                .builder()
                .build(key, bucketConfiguration())
                .tryConsume(1);
        } catch (Exception e) {
            log.error("Rate limiter Redis error for IP {}: {}", ipAddress, e.getMessage());
            return true; // fail open — don't block users if Redis is down
        }
    }
}
