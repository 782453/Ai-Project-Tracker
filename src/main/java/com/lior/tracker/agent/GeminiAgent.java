package com.lior.tracker.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lior.tracker.ChatMessage;
import com.lior.tracker.model.MessagesMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class GeminiAgent implements AiAgent {
    private final String name = "Gemini";
    public Result ask(List<ChatMessage> messages, String ver) throws IOException, InterruptedException {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if(apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("\nGEMINI_API_KEY environment variable is required.");
        }
        String url;
        if (ver.equals("default")) url = "https://generativelanguage.googleapis.com/v1/models/gemini-3.5-flash:generateContent?key=" + apiKey;
        else url = "https://generativelanguage.googleapis.com/v1/models/gemini-3.5-flash-lite:generateContent?key=" + apiKey;
        MessagesMapper mM = new MessagesMapper();
        ObjectNode contentsArray = mM.GeminiBuildContentsNode(messages);
        String requestBody = mM.getMapper().writeValueAsString(contentsArray);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        //System.out.println(response.body()); //REMOVE TO SEE FULL BODY
        if (response.statusCode() == 429) {
            throw new RuntimeException("API quota/rate limit exceeded: ");
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException("API error " + response.statusCode() + ": " + response.body());
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());
        int totalTokens = root.get("usageMetadata").get("totalTokenCount").asInt();
        return new Result(root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText(),
        totalTokens);
    }
    public String getName() {return this.name;}
}
