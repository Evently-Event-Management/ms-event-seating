package com.ticketly.mseventseating.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.event.CreateEventRequest;
import com.ticketly.mseventseating.dto.event.EventResponseDTO;
import com.ticketly.mseventseating.service.event.EventCreationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventCreationService eventCreationService;
    private final ObjectMapper objectMapper; // To convert JSON string to DTO

    // ✅ Updated endpoint to consume multipart/form-data
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<EventResponseDTO> createEvent(
            @RequestPart("request") @Valid String requestStr,
            @RequestPart(value = "coverImages", required = false) MultipartFile[] coverImages,
            @AuthenticationPrincipal Jwt jwt) throws IOException {

        // Convert the JSON string part into our DTO
        CreateEventRequest request = objectMapper.readValue(requestStr, CreateEventRequest.class);

        String userId = jwt.getSubject();
        EventResponseDTO createdEvent = eventCreationService.createEvent(request, coverImages, userId, jwt);
        return new ResponseEntity<>(createdEvent, HttpStatus.CREATED);
    }
}
