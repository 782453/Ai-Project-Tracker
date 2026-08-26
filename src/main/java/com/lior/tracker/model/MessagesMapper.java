package com.lior.tracker.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lior.tracker.Chat;
import com.lior.tracker.ChatMessage;

import java.util.List;

public class MessagesMapper {
    private final ObjectMapper mapper = new ObjectMapper();
    private ObjectNode GeminiMessageToNode(ChatMessage message) {
        ObjectNode messageNode = mapper.createObjectNode();
        messageNode.put("role", message.getRole());
        ArrayNode partsArray = mapper.createArrayNode();
        ObjectNode partNode = mapper.createObjectNode();
        partNode.put("text", message.getText());
        partsArray.add(partNode);
        if(message.getInline_data() != null) {
            ObjectNode inlineDataNode = mapper.createObjectNode();
            inlineDataNode.put("mime_type", message.getMimeType());
            inlineDataNode.put("data", message.getInline_data());
            ObjectNode filePartNode = mapper.createObjectNode();
            filePartNode.put("inline_data", inlineDataNode);
            partsArray.add(filePartNode);
        }
        messageNode.set("parts", partsArray);
        return messageNode;
    }
    public ObjectNode GeminiBuildContentsNode(List<ChatMessage> messages) {
        ArrayNode contentsArray = mapper.createArrayNode();
        for (ChatMessage message : messages) {
            contentsArray.add(GeminiMessageToNode(message));
        }
        ObjectNode messagesNode = mapper.createObjectNode();
        messagesNode.set("contents", contentsArray);
        return messagesNode;
    }
    public ObjectMapper getMapper() {return mapper;}
    public ObjectNode GroqMessageToNode(ChatMessage message) {
        ObjectNode messageNode = mapper.createObjectNode();
        messageNode.put("role", (message.getRole().equals("model")) ?  "assistant" : message.getRole());
        messageNode.put("content", message.getText());
        return messageNode;
    }
    public ObjectNode GroqBuildMessagesNode(List<ChatMessage> messages) {
        ArrayNode messagesArray = mapper.createArrayNode();
        for (ChatMessage message : messages) {
            messagesArray.add(GroqMessageToNode(message));
        }
        ObjectNode messagesNode = mapper.createObjectNode();
        messagesNode.put("model", Chat.getGroqModel());
        messagesNode.set("messages", messagesArray);
        return messagesNode;
    }
}
