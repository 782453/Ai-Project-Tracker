package com.lior.tracker.gui;

import com.lior.tracker.Chat;
import com.lior.tracker.ChatMessage;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class HistoryWindow {
    public static void open(List<ChatMessage> messages) {
        List<ChatMessage> snapshot = List.copyOf(messages);
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Chat History");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(700, 600);
            frame.setLocationRelativeTo(null);
            JTextArea textArea = new JTextArea();
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            if(messages.isEmpty()) textArea.append("No messages");
            else for(ChatMessage message : snapshot) {
                textArea.append(formatMessage(message));
                textArea.append("\n\n");
            }

            JScrollPane scrollPane = new JScrollPane(textArea);
            frame.add(scrollPane);
            frame.setVisible(true);
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }
    public static String formatMessage(ChatMessage message) {
        String role = switch (message.getRole()) {
            case "user" -> "YOU";
            case "assistant", "model" -> Chat.getAgentName();
            case "system" -> "SYSTEM";
            default -> message.getRole().toUpperCase();
        };
        return "[" + role + "]\n" + message.getText();
    }
}
