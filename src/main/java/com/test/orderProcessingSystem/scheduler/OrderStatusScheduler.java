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
        int updatedCount = adminOrderService.movePendingOrdersToProcessing();
        if (updatedCount > 0) {
            log.info("Moved {} order(s) from PENDING to PROCESSING", updatedCount);
        }
    }
}
