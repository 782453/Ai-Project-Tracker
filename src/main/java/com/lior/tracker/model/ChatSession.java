package com.lior.tracker.model;

import com.lior.tracker.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class ChatSession {
    private List<Project> projects;
    private List<ChatMessage> messages;
    private String userMessage;
    private int tokens;
    public ChatSession(List<Project> projects, List<ChatMessage> messages, String userMessage, int tokens) {
        this.projects = projects;
        this.messages = messages;
        this.userMessage = userMessage;
        this.tokens = tokens;
    }

    public List<Project> getProjects() {return projects;}
    public List<ChatMessage> getMessages() {return messages;}
    public List<ChatMessage> getLastTwoMessage() {
        List<ChatMessage> lastTwoMessages = new ArrayList<>();
        lastTwoMessages.add(messages.get(messages.size()-2));
        lastTwoMessages.add(messages.getLast());
        return lastTwoMessages;
    }
    public String getUserMessage() {return userMessage;}
    public int getTokens() {return tokens;}
    public void setProjects(List<Project> projects) {this.projects = projects;}
    public void setMessages(List<ChatMessage> messages) {this.messages = messages;}
    public void setUserMessage(String userMessage) {this.userMessage = userMessage;}
    public void setTokens(int tokens) {this.tokens = tokens;}
}
