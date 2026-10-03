package com.lior.tracker.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lior.tracker.ChatMessage;
import com.lior.tracker.agent.AiAgent;
import com.lior.tracker.agent.Result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MemoryExtractor {
    public Optional<MemoryCandidate> extract(String userMessage,String agentReply, AiAgent agent) throws IOException, InterruptedException {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("user", userMessage));
        messages.add(new ChatMessage("model", agentReply));
        messages.add(new ChatMessage("user", """
            Analyze only the latest exchange and decide whether it introduced
            information worth storing in long-term memory.
            Worth remembering:
            - everything the user says to remember
            - important facts
            - preferences
            - decisions
            - technical information
            - project information
            - persistent constraints
            Do not remember temporary or unimportant conversation.
            Return ONLY valid JSON. No markdown or explanation.
            If nothing should be remembered:
            {
            "remember":false
            } (make sure you go to next line after '}')
            
            Otherwise:
                Choose a short project/topic name based on the exchange.
                If it isn't related to a specific project, use "general"
            {
              "remember": true,
              "title": "...",
              "content": "...",
              "type": "FACT|PREFERENCE|DECISION|TECHNICAL|PROJECT|PERSON|GENERAL",
              "project": "...",
              "importance": 1,
              "tags": ["tag1", "tag2"]
              "related": ["[[rel1]]", "[[rel2]]"]
            }
            the related are common related tags (should also include at least one of the types)
            """));
        Result reply = null;
        while(reply == null){
            reply = agent.ask(messages, "default");
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(reply.text().trim());
        if (!json.get("remember").asBoolean()) {
            return Optional.empty();
        }
        List<String> tags = new ArrayList<>();
        List<String> related = new ArrayList<>();
        json.get("tags").forEach(tag -> tags.add(tag.asText()));
        json.get("related").forEach(relate -> related.add(relate.asText()));
        MemoryCandidate candidate = new MemoryCandidate(
                json.get("title").asText(),
                json.get("content").asText(),
                MemoryType.valueOf(
                        json.get("type").asText().toUpperCase(Locale.ROOT)
                ),
                json.get("project").asText(),
                json.get("importance").asInt(),
                tags,
                related
        );
        return Optional.of(candidate);
    }
}
