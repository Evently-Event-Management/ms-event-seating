package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.event.DiscountRequestDTO;
import com.ticketly.mseventseating.dto.event.DiscountDetailsDTO;
import com.ticketly.mseventseating.service.discount.DiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/events/{eventId}/discounts")
@RequiredArgsConstructor
@Slf4j
public class DiscountController {

    private final DiscountService discountService;

    @PostMapping
    public ResponseEntity<DiscountDetailsDTO> createDiscount(
            @PathVariable UUID eventId,
            @RequestBody @Valid DiscountRequestDTO requestDTO,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.info("Creating discount for event: {}", eventId);
        String userId = jwt.getSubject();
        DiscountDetailsDTO createdDiscount = discountService.createDiscount(eventId, requestDTO, userId);
        return new ResponseEntity<>(createdDiscount, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<DiscountDetailsDTO>> getDiscounts(
            @PathVariable UUID eventId,
            @RequestParam(defaultValue = "false") boolean includePrivate,
            @PageableDefault Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.info("Getting discounts for event: {}, includePrivate: {}", eventId, includePrivate);
        String userId = jwt.getSubject();
        Page<DiscountDetailsDTO> discounts = discountService.getDiscounts(eventId, includePrivate, userId, pageable);
        return ResponseEntity.ok(discounts);
    }

    @GetMapping("/{discountId}")
    public ResponseEntity<DiscountDetailsDTO> getDiscount(
            @PathVariable UUID eventId,
            @PathVariable UUID discountId,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.info("Getting discount: {} for event: {}", discountId, eventId);
        String userId = jwt.getSubject();
        DiscountDetailsDTO discount = discountService.getDiscount(eventId, discountId, userId);
        return ResponseEntity.ok(discount);
    }

    @PutMapping("/{discountId}")
    public ResponseEntity<DiscountDetailsDTO> updateDiscount(
            @PathVariable UUID eventId,
            @PathVariable UUID discountId,
            @RequestBody @Valid DiscountRequestDTO requestDTO,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.info("Updating discount: {} for event: {}", discountId, eventId);
        String userId = jwt.getSubject();
        DiscountDetailsDTO updatedDiscount = discountService.updateDiscount(eventId, discountId, requestDTO, userId);
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
