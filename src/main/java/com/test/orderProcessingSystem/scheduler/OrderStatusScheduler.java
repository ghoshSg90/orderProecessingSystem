package com.test.orderProcessingSystem.scheduler;

import com.test.orderProcessingSystem.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusScheduler {

    private static final long FIVE_MINUTES_MS = 5 * 60 * 1000;

    private final AdminOrderService adminOrderService;

    @Scheduled(fixedRate = FIVE_MINUTES_MS)
    public void movePendingOrdersToProcessing() {
        long startTime = System.currentTimeMillis();
        log.debug("Order status scheduler started");
        try {
            int updatedCount = adminOrderService.movePendingOrdersToProcessing();
            long durationMs = System.currentTimeMillis() - startTime;
            if (updatedCount > 0) {
                log.info("Order status scheduler completed updated={} durationMs={}", updatedCount, durationMs);
            } else {
                log.debug("Order status scheduler completed updated=0 durationMs={}", durationMs);
            }
        } catch (Exception ex) {
            log.error("Scheduler execution failed", ex);
        }
    }
}
