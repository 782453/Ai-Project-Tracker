package com.lior.tracker.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lior.tracker.Chat;
import com.lior.tracker.ChatMessage;
import com.lior.tracker.model.MessagesMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class NvidiaAgent implements AiAgent {
    private static final boolean stream = true;
    public static boolean isStream() {return stream;}
    public Result ask(List<ChatMessage> messages, String ver) throws IOException, InterruptedException {
        String sendFile;
        if (messages.getLast().getInline_data() != null) {
            sendFile = sendFileToGemini(messages.getLast());
            messages.add(new ChatMessage("model", sendFile));
            messages.add(new ChatMessage("user", "I didn't see your previous message so i need you to reprocess it and send it back to me."));
        }
        String apiKey = System.getenv("NVIDIA_API_KEY");
        if(apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("\nNVIDIA_API_KEY environment variable is required.");
        }
        String url = "https://integrate.api.nvidia.com/v1/chat/completions";
        MessagesMapper mM = new MessagesMapper();
        ObjectNode messagesArray = mM.NvidiaBuildMessagesNode(messages);
        String requestBody = mM.getMapper().writeValueAsString(messagesArray);

        HttpClient client = HttpClient.newHttpClient(); //thinking about making it one for all the calls
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        if(this.stream) return sendStream(request);
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
        int totalTokens = root.get("usage").get("total_tokens").asInt();
        return new Result(root.path("choices").get(0).path("message").path("content").asText(), totalTokens);
    }

    public String getName() {
        return "Nemotron";
    }
    public String sendFileToGemini(ChatMessage message) throws IOException, InterruptedException {
        List<ChatMessage> base64File = new ArrayList<ChatMessage>();
        base64File.add(message);
        AiAgent agent = new GeminiAgent();
        Result reply = new Result("", 0);
        try {
            reply = agent.ask(base64File, "default");
        } catch (RuntimeException e) {
            try {
                Thread.sleep(20);
                reply = agent.ask(base64File, "lite");
            } catch (RuntimeException e1) {
                System.err.println(e1.getMessage());
            }
        }
        return reply.text();
    }
    public Result sendStream(HttpRequest request) throws IOException, InterruptedException {
        boolean first = true;
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if(response.statusCode() != 200) {
            String errorBody;
            try (InputStream body = response.body()) {
                errorBody = new String(body.readAllBytes(), StandardCharsets.UTF_8);
            }
            throw new RemoteException("API error " + response.statusCode() + ": " + errorBody);
        }
        StringBuilder fullResponse = new StringBuilder();
        int totalTokens = 0;
        ObjectMapper mapper = new ObjectMapper();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if(line.isBlank() || !line.startsWith("data")) continue;
                String data = line.substring(5).trim();
                if(data.equals("[DONE]")) break;
                JsonNode root = mapper.readTree(data);
                JsonNode usage = root.path("usage");
                if(!usage.isMissingNode() && usage.has("total_tokens")) {
                    totalTokens = usage.path("total_tokens").asInt();
                }
                JsonNode choises = root.path("choices");
                if(!choises.isArray() || choises.isEmpty()) continue;
                JsonNode delta = choises.get(0).path("delta");
                JsonNode contentNode = delta.get("content");
                if(contentNode == null || contentNode.isNull()) continue;
                String chunk = contentNode.asText();
                if (chunk.isEmpty()) continue;
                if (first) {
                    System.out.println("\n" + Chat.getAgentName() + ": ");
                    first = false;
                }
                System.out.print(chunk);
                System.out.flush();
                fullResponse.append(chunk);
            }
            return new Result(fullResponse.toString(), totalTokens);
        }
    }
}
