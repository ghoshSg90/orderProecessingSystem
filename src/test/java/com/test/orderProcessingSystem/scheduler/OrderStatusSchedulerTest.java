package com.test.orderProcessingSystem.scheduler;

import com.test.orderProcessingSystem.service.AdminOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStatusSchedulerTest {

    @Mock
    private AdminOrderService adminOrderService;

    @InjectMocks
    private OrderStatusScheduler orderStatusScheduler;

    @Test
    void scheduledTask_delegatesToServiceWhenOrdersMoved() {
        when(adminOrderService.movePendingOrdersToProcessing()).thenReturn(3);

        orderStatusScheduler.movePendingOrdersToProcessing();

        verify(adminOrderService, times(1)).movePendingOrdersToProcessing();
    }

    @Test
    void scheduledTask_delegatesToServiceWhenNoOrders() {
        when(adminOrderService.movePendingOrdersToProcessing()).thenReturn(0);

        orderStatusScheduler.movePendingOrdersToProcessing();

        verify(adminOrderService, times(1)).movePendingOrdersToProcessing();
    }
}
