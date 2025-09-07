package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.event.GenerateOverviewRequest;
import com.ticketly.mseventseating.dto.event.GenerateOverviewResponse;
import com.ticketly.mseventseating.service.ai.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/ai")
@RequiredArgsConstructor
public class AiController {
    private final GeminiService geminiService;

    /**
     * Generates an event overview in markdown format using AI based on the provided request details.
     *
     * @param request The request containing event details and prompt for AI generation.
     * @return The generated overview as a markdown response, or 500 error if generation fails.
     */
    @PostMapping("/generate-overview")
    public ResponseEntity<GenerateOverviewResponse> generateOverview(@RequestBody GenerateOverviewRequest request) {
        try {
            String markdown = geminiService.generateEventOverview(request.title(), request.organization(), request.description(), request.category(), request.prompt());
            return ResponseEntity.ok(new GenerateOverviewResponse(markdown));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
