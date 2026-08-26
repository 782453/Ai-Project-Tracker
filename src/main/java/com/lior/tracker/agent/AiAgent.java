package com.lior.tracker.agent;

import com.lior.tracker.ChatMessage;

import java.io.IOException;
import java.util.List;

public interface AiAgent {
    Result ask(List<ChatMessage> messages, String ver) throws IOException, InterruptedException;
    String getName();
}
