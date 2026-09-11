package com.lior.tracker.model;

import com.lior.tracker.Chat;
import com.lior.tracker.ChatMessage;
import com.lior.tracker.gui.HistoryWindow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Scanner;

public class ProjectCommands2 {
    public static ChatSession commands(ChatSession session) throws IOException {
        Scanner input = Chat.getInput();
        switch (session.getUserMessage()) {
            case "/++":
                System.out.println("""
                        \tCommands [PG 2/2]:
                        \t/history - View chat history
                        \t/fhistory - View entire session history
                        \t/rules - Change session rules
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
                Chat.getRules().getRules();
                System.out.print("Edit chat rules (y/n)? ");
                if (input.nextLine().equalsIgnoreCase("y")) Chat.setRules(new Rules());
                else break;
                if(Chat.getRules().isMaxTokens()) {
                    session.getMessages().add(new ChatMessage("user", Chat.getRules().getMaxPrompt()));
                    session.getMessages().add(new ChatMessage("model", "Memory updated!"));
                }
                break;
            case "/exit":
                System.exit(0);
                break;
            case "/..":
                return session;
            default:
                System.out.println("'" + session.getUserMessage() + "' is not a valid command");
                break;
        }
        return session;
    }
}
