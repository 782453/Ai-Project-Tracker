package com.lior.tracker.model;

import com.lior.tracker.Chat;
import com.lior.tracker.ChatMessage;
import com.lior.tracker.gui.HistoryWindow;
import com.lior.tracker.memory.MemoryType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class ProjectCommands2 {
    public static ChatSession commands(ChatSession session) {
        Scanner input = Chat.getInput();
        switch (session.getUserMessage()) {
            case "/++":
                System.out.println("""
                        \tCommands [PG 2/2]:
                        \t/history - View chat history
                        \t/fhistory - View entire session history
                        \t/rules - Change session rules
                        \t/resend - Resend last message
                        \t/exit - Exit the program
                        \t/.. - Previous page""");
                break;
            case "/history":
                HistoryWindow.open(session.getMessages());
                break;
            case "/fhistory":
                HistoryWindow.open(Chat.getHistory());
                break;
            case "/rules":
                System.out.println(Chat.getRules().toString());
                System.out.print("Edit chat rules (y/n)? ");
                if (input.nextLine().equalsIgnoreCase("y")) Chat.setRules(new Rules());
                else break;
                if(Chat.getRules().isMaxTokens()) {
                    if(Chat.getAgentName().equals("Nemotron")) session.getMessages().add(new ChatMessage("system", Chat.getRules().getMaxPrompt()));
                    else {
                        session.getMessages().add(new ChatMessage("user", Chat.getRules().getMaxPrompt()));
                        session.getMessages().add(new ChatMessage("model", "Memory updated!"));
                    }
                }
                break;
            case "/resend":
                if (session.getMessages().isEmpty()) break;
                if(session.getMessages().getLast().getRole().equals("model")) break;
                ProjectCommands.setFlag(true);
                session.setUserMessage(session.getMessages().getLast().getText());
                session.getMessages().removeLast();
                break;
            case "/memory add":
                System.out.print("Memory title: ");
                String title = Chat.getInput().nextLine();
                System.out.println("Memory content (type 'eof' to finish):");
                StringBuilder contentBuilder = new StringBuilder();
                String line;
                while (!(line = Chat.getInput().nextLine()).equalsIgnoreCase("eof")) {
                    if (!contentBuilder.isEmpty()) contentBuilder.append("\n");
                    contentBuilder.append(line);
                }
                System.out.println("Memory types:");
                for (MemoryType type : MemoryType.values()) System.out.println(type);
                System.out.print("Memory type: ");
                MemoryType memoryType = MemoryType.valueOf(Chat.getInput().nextLine().trim().toUpperCase(Locale.ROOT));
                System.out.print("Project name: ");
                String projectName = Chat.getInput().nextLine();
                System.out.print("Importance (1-10): ");
                int importance = Integer.parseInt(Chat.getInput().nextLine());
                System.out.print("Memory tags: ");
                String tagInput = Chat.getInput().nextLine().trim();
                List<String> tags = tagInput.isBlank() ? List.of() : List.of(tagInput.split("\\s+"));
                System.out.print("Memory related tags: ");
                String relateTagInput = Chat.getInput().nextLine().trim();
                List<String> related = relateTagInput.isBlank() ? List.of() : List.of(relateTagInput.split("\\s+"));
                try {
                    Chat.getMemoryService().remember(
                            title,
                            contentBuilder.toString(),
                            memoryType,
                            projectName,
                            importance,
                            tags,
                            related
                    );
                    System.out.println("Memory saved.");
                } catch (IOException e) {
                    System.out.println("Failed to save memory.");
                }
                break;
            case "/exit":
                System.exit(0);
                break;
            case "/..":
                session.setUserMessage("/?");
                return session;
            default:
                System.out.println("'" + session.getUserMessage() + "' is not a valid command");
                break;
        }
        return session;
    }
}
