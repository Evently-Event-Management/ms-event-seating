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

    @KafkaListener(topics = "ticketly.seats.booked", errorHandler = "kafkaErrorHandler")
    public void onSeatsBooked(@Payload SeatStatusChangeEventDto payload, Acknowledgment acknowledgment) {
        log.info("Received SeatsBooked event for session: {}", payload.session_id());
        try {
            // Delegate the business logic to the service layer
            seatBookingService.processSeatsBooked(payload);

            // Acknowledge the message upon successful processing
            acknowledgment.acknowledge();
            log.info("Successfully acknowledged SeatsBooked event for session {}", payload.session_id());
        } catch (Exception e) {
            // Log the error
            log.error("Error processing SeatsBooked event for session {}. Error: {}",
                    payload.session_id(), e.getMessage(), e);

            // Acknowledge the message even on error to prevent getting stuck
            // This will prevent the infinite loop on business logic errors
            acknowledgment.acknowledge();
            log.info("Acknowledged message despite error for session {} to prevent infinite retries",
                    payload.session_id());
        }
    }
}