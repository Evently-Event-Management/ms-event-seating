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
//        5s delay in gemini response
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//        return "# Acme Inc's Annual Innovation Festival 🚀\n\nGet ready for an unforgettable 5-day journey into the future of innovation! Acme Inc is thrilled to present our most ambitious event yet, a vibrant celebration of creativity, technology, and groundbreaking ideas. Join us for a spectacular festival designed to inspire, connect, and propel you forward.\n\n---\n\n## About the Event ✨\n\nAcme Inc's Annual Innovation Festival is a premier gathering for forward-thinkers, creators, and anyone passionate about shaping tomorrow. For five exhilarating days, we're transforming [Venue Name/City] into a hub of discovery, collaboration, and inspiration. Whether you're looking to gain cutting-edge knowledge, network with industry leaders, or simply immerse yourself in the latest trends, this festival has something for everyone aged 16 and above. Prepare to be amazed by a curated experience that blends insightful discussions, hands-on workshops, and electrifying entertainment.\n\n---\n\n## Agenda Highlights 📅\n\nOur meticulously crafted agenda is packed with diverse sessions to ignite your imagination:\n\n*   **Day 1: Visionary Keynotes & Future Trends**\n    *   Opening address by Acme Inc CEO\n    *   Panel: \"The Next Decade of Disruptive Technologies\"\n    *   Interactive workshops on AI integration\n*   **Day 2: Design Thinking & Creative Problem Solving**\n    *   Masterclass: \"Unlocking Your Creative Potential\"\n    *   Ideation labs for sustainable solutions\n    *   Showcase of innovative product designs\n*   **Day 3: Tech Deep Dives & Emerging Platforms**\n    *   Breakout sessions on quantum computing and blockchain\n    *   Demonstrations of cutting-edge software\n    *   Networking reception with tech pioneers\n*   **Day 4: Entrepreneurship & Startup Ecosystem**\n    *   Pitch competitions and investor meetups\n    *   Workshops on scaling your business\n    *   Fireside chats with successful founders\n*   **Day 5: Innovation in Action & Community Building**\n    *   Live demonstrations of implemented innovations\n    *   Community project showcases\n    *   Closing celebration and future outlook\n\n---\n\n## Who Should Attend? 🤔\n\nThis festival is tailor-made for:\n\n*   **Students & Young Innovators (16+):** Aspiring entrepreneurs, tech enthusiasts, and future leaders eager to learn and explore new possibilities.\n*   **Professionals & Industry Experts:** Individuals seeking to stay ahead of the curve, discover new tools, and expand their professional network.\n*   **Creatives & Designers:** Artists, designers, and problem-solvers looking for inspiration and new approaches to their craft.\n*   **Entrepreneurs & Business Leaders:** Anyone interested in growth, innovation, and connecting with a vibrant ecosystem of like-minded individuals.\n*   **Technology Enthusiasts:** Individuals passionate about the latest advancements and eager to understand their impact.\n\n---\n\n## Key Takeaways 🔑\n\nBy attending Acme Inc's Annual Innovation Festival, you will:\n\n*   **Gain Insights:** Learn from leading experts and gain a deep understanding of emerging technologies and market trends.\n*   **Spark Creativity:** Discover new methodologies and tools to foster innovation in your personal and professional life.\n*   **Expand Your Network:** Connect with peers, mentors, and potential collaborators from diverse backgrounds.\n*   **Develop Skills:** Participate in hands-on workshops to acquire practical skills and knowledge.\n*   **Get Inspired:** Leave feeling motivated, empowered, and ready to implement your own innovative ideas.\n\n---\n\nDon't miss out on this incredible opportunity to be part of the innovation revolution!\n\n**Get your tickets now on Ticketly!** 🎟️";
    }
}