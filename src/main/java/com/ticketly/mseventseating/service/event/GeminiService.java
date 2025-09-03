package com.ticketly.mseventseating.service.event;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final VertexAiGeminiChatModel chatModel;

    /**
     * This method generates an event overview in Markdown format using the Gemini model.
     * It uses prompt engineering to create a detailed and engaging overview based on the provided event details
     * and user prompt.
     *
     * @param title        The title of the event.
     * @param organization The organization hosting the event.
     * @param description  A brief description of the event.
     * @param category     The category of the event (e.g., Music, Tech, Art).
     * @param userPrompt   Additional user-defined requirements for the overview.
     * @return A string containing the event overview in Markdown format.
     */
    public String generateEventOverview(String title, String organization, String description, String category, String userPrompt) {
        // --- This is the Prompt Engineering part! ---
        StringBuilder enhancedPrompt = new StringBuilder();
        enhancedPrompt.append("""
                You are a world-class event marketing copywriter. Your task is to write a compelling and engaging event overview in Markdown format based ONLY on the details provided below.
                
                **Core Instructions:**
                - The main title should prominently feature the event and the organization hosting it.
                - Use headings, bullet points, and bold text for a clear, readable structure.
                - Incorporate relevant and tasteful emojis (like 🎤, 📅, 📍, ✨).
                - The tone must be exciting, professional, and tailored to the event's category.
                - Create standard marketing sections like "About the Event" and "Who Should Attend?".
                - Conclude the entire overview with a call to action to get tickets on Ticketly.
                - The final output must be ONLY the Markdown content, without any extra conversation or explanations.
                
                **CRITICAL: Do NOT invent specific details. Your job is to create a professional template based ONLY on the information provided. Specifically:**
                - **Do not guess agenda items, times, or session titles.** Create a generic placeholder for the agenda if no details are provided.
                - **Do not invent venue names or location details.** Omit this section entirely unless the organizer provides them in their instructions.
                - **Do not invent ticket prices or pricing tiers.** Omit the pricing section entirely unless the organizer provides them in their instructions.
                - **Do not invent any links or URLs.**
                - **Do not include any disclaimers or notes about AI generation.**
                
                ---
                **EVENT DETAILS:**
                """);

        // Add event details if available to provide context to the model
        if (title != null && !title.isEmpty()) {
            enhancedPrompt.append("\n- Event Title: ").append(title);
        }

        if (organization != null && !organization.isEmpty()) {
            enhancedPrompt.append("\n- Hosted By: ").append(organization);
        }

        if (description != null && !description.isEmpty()) {
            enhancedPrompt.append("\n- Core Description: ").append(description);
        }

        if (category != null && !category.isEmpty()) {
            enhancedPrompt.append("\n- Event Category: ").append(category);
        }

        if (userPrompt != null && !userPrompt.isEmpty()) {
            enhancedPrompt.append("\n- Additional User Instructions: ").append(userPrompt);
        }

        enhancedPrompt.append("\n---");

        return chatModel.call(enhancedPrompt.toString());
    }
}