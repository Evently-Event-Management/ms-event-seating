package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.event.DiscountRequestDTO;
import com.ticketly.mseventseating.dto.event.DiscountResponseDTO;
import com.ticketly.mseventseating.service.discount.DiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/events/{eventId}/discounts")
@RequiredArgsConstructor
@Slf4j
public class DiscountController {

    private final DiscountService discountService;

    @PostMapping
    public ResponseEntity<DiscountResponseDTO> createDiscount(
            @PathVariable UUID eventId,
            @RequestBody @Valid DiscountRequestDTO requestDTO,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.info("Creating discount for event: {}", eventId);
        String userId = jwt.getSubject();
        DiscountResponseDTO createdDiscount = discountService.createDiscount(eventId, requestDTO, userId);
        return new ResponseEntity<>(createdDiscount, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<DiscountResponseDTO>> getDiscounts(
            @PathVariable UUID eventId,
            @RequestParam(defaultValue = "false") boolean includePrivate,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.info("Getting discounts for event: {}, includePrivate: {}", eventId, includePrivate);
        String userId = jwt.getSubject();
        List<DiscountResponseDTO> discounts = discountService.getDiscounts(eventId, includePrivate, userId);
        return ResponseEntity.ok(discounts);
    }

    @GetMapping("/{discountId}")
    public ResponseEntity<DiscountResponseDTO> getDiscount(
            @PathVariable UUID eventId,
            @PathVariable UUID discountId,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.info("Getting discount: {} for event: {}", discountId, eventId);
        String userId = jwt.getSubject();
        DiscountResponseDTO discount = discountService.getDiscount(eventId, discountId, userId);
        return ResponseEntity.ok(discount);
    }

    @PutMapping("/{discountId}")
    public ResponseEntity<DiscountResponseDTO> updateDiscount(
            @PathVariable UUID eventId,
            @PathVariable UUID discountId,
            @RequestBody @Valid DiscountRequestDTO requestDTO,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.info("Updating discount: {} for event: {}", discountId, eventId);
        String userId = jwt.getSubject();
        DiscountResponseDTO updatedDiscount = discountService.updateDiscount(eventId, discountId, requestDTO, userId);
        return ResponseEntity.ok(updatedDiscount);
    }

    @DeleteMapping("/{discountId}")
    public ResponseEntity<Void> deleteDiscount(
            @PathVariable UUID eventId,
            @PathVariable UUID discountId,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.info("Deleting discount: {} for event: {}", discountId, eventId);
        String userId = jwt.getSubject();
        discountService.deleteDiscount(eventId, discountId, userId);
        return ResponseEntity.noContent().build();
    }
}
