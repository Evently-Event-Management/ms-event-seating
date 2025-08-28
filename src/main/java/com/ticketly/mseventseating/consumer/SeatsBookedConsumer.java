package com.ticketly.mseventseating.consumer;

import com.ticketly.mseventseating.dto.event.SeatStatusChangeEventDto;
import com.ticketly.mseventseating.service.event.SeatBookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SeatsBookedConsumer {

    private final SeatBookingService seatBookingService;

    @KafkaListener(topics = "ticketly.seats.booked")
    public void onSeatsBooked(@Payload SeatStatusChangeEventDto payload, Acknowledgment acknowledgment) {
        log.info("Received SeatsBooked event for session: {}", payload.sessionId());
        try {
            // Delegate the business logic to the service layer
            seatBookingService.processSeatsBooked(payload);

            // Acknowledge the message upon successful processing
            acknowledgment.acknowledge();
            log.info("Successfully acknowledged SeatsBooked event for session {}", payload.sessionId());
        } catch (Exception e) {
            // Log the error and DO NOT acknowledge the message.
            // Kafka will redeliver the message based on your retry configuration.
            log.error("Error processing SeatsBooked event for session {}. Message will be retried. Error: {}",
                    payload.sessionId(), e.getMessage(), e);
        }
    }
}