package com.ticketly.mseventseating.consumer;

import com.ticketly.mseventseating.dto.event.OrderUpdatedEventDto;
import com.ticketly.mseventseating.service.order.OrderProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderUpdatedConsumer {

    private final OrderProcessingService orderProcessingService;

    @KafkaListener(topics = "ticketly.order.updated", errorHandler = "kafkaErrorHandler")
    public void onOrderUpdated(@Payload OrderUpdatedEventDto payload, Acknowledgment acknowledgment) {
        log.info("Received OrderUpdated event with status: {}, orderID: {}", 
                payload.Status(), payload.OrderID());
        try {
            // Delegate the business logic to the service layer
            orderProcessingService.processOrderUpdate(payload);

            // Acknowledge the message upon successful processing
            acknowledgment.acknowledge();
            log.info("Successfully acknowledged OrderUpdated event for order {}", payload.OrderID());
        } catch (Exception e) {
            // Log the error
            log.error("Error processing OrderUpdated event for order {}. Error: {}",
                    payload.OrderID(), e.getMessage(), e);

            // Acknowledge the message even on error to prevent getting stuck
            // This will prevent the infinite loop on business logic errors
            acknowledgment.acknowledge();
            log.info("Acknowledged message despite error for order {} to prevent infinite retries",
                    payload.OrderID());
        }
    }
}