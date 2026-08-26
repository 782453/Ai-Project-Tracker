package com.lior.tracker.model;

import com.lior.tracker.ChatMessage;
import com.lior.tracker.agent.*;

import java.io.*;
//import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class temp {
    private static final Scanner input = new Scanner(System.in);
    private static List<Project> projects;
    private static List<ChatMessage> messages;
    private static boolean summarize = false;
    private static final String SUMM_PROMPT = """
                        Summarize the conversation compactly for use as future AI context.
                        Preserve:
                        -important facts
                        -user preferences
                        -decisions already made
                        -technical details
                        -variable/class/API/model names
                        -constraints
                        -unresolved questions and tasks
                        -last few messages of our conversation
                        Remove:
                        -repetition
                        -greetings
                        -filler
                        -irrelevant conversation
                        Do not invent information
                        Reply only with the summarization, nothing else.
                        """;
    public static void main(String[] args) throws IOException, InterruptedException, NullPointerException {
        AiAgent agent = new GeminiAgent();
        messages = new ArrayList<>();
        chat(agent);
/*
  int port = 5000;
  *        List<Project> projects = getProjects();
  *        ServerSocket serverSocket = new ServerSocket(port);
  *        while (true) {
  *            Socket clientSocket = serverSocket.accept();
  *            Thread thread = new Thread(() -> {
  *                try {
  *                    handleClient(clientSocket);
  *                }catch (IOException e) {
  *                    e.printStackTrace();
  *                }
  *            });
  *            thread.start();
  *        }
 */
    }
    public static void handleClient(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        out.println("Connected to " + clientSocket.getInetAddress().getHostName());
        String line;
        while ((line = in.readLine()) != null) {
            System.out.println("Received: " + line);
            out.println("Server received: " + line);
        }
    }
    public static String commands(String userMessage, int tokens) throws IOException {
        int num;
        boolean isPaste = false;
        switch (userMessage) {
            case "/?":
                System.out.println("""
                        \tCommands:
                        \t/nchat - Start a fresh chat
                        \t/nproject - Create a new project
                        \t/projects - List and project options
                        \t/exit - Exit the program
                        \t/summarize - Summarize the chat""");
                break;
            case "/nchat":
                messages = new ArrayList<>();
                break;
            case "/nproject":
                projects = Project.getProjects(false);
                System.out.print("Project name: ");
                String name = input.nextLine();
                System.out.print("Project status (ACTIVE, BLOCKED, PAUSED, DONE): ");
                String status = input.nextLine();
                System.out.print("Last updated (YYYY-MM-DD): ");
                String lastUpdated = input.nextLine();
                System.out.print("Project notes: ");
                String notes = input.nextLine();
                projects.add(new Project(name, status, lastUpdated, notes));
                System.out.println("Project added!");
                break;
            case "/projects":
                if(projects == null) projects = Project.getProjects(true);
                else for (int i = 0; i < projects.size(); i++) System.out.println("[" + i + "] " + projects.get(i).getName() +
                        " - " + projects.get(i).getStatus());
                System.out.print("Select a project: ");
                num = Integer.parseInt(input.nextLine());
                if(num >=0 && num < projects.size()) {
                    System.out.println("Selected project: " + projects.get(num).getName());
                    System.out.print("[w] Work on project, [c] Change project status, [a] Add notes, [DEL] Delete project\nOption: ");
                    //input.nextLine();
                    String proj = input.nextLine();
                    switch (proj) {
                        case "w":
                            messages.add(new ChatMessage("user", "We'll continue working on this project today: " + projects.get(num).getName() +
                                    "\nNotes: " + projects.get(num).getNotes()));
                            messages.add(new ChatMessage("model", "OK!"));
                            System.out.println("Currently working on: " + projects.get(num).getName());
                            break;
                        case "c":
                            System.out.print("Select status (ACTIVE, BLOCKED, PAUSED, DONE): ");
                            projects.get(num).setStatus(input.nextLine());
                            System.out.println("Project status changed!");
                            break;
                        case "a":
                            System.out.print("Write notes to add: ");
                            projects.get(num).setNotes(input.nextLine());
                            System.out.println("Notes added!");
                            break;
                        case "DEL":
                            projects.remove(num);
                            System.out.println("Project removed!");
                            break;
                        default:
                            System.out.println("Invalid choice");
                            break;
                    }
                }
                break;
            case "/paste":
                userMessage = paste();
                input.nextLine();
                isPaste = true;
                break;
            case "/summarize":
                if(tokens >= 5000) {
                    summarize = true;
                    return SUMM_PROMPT;
                } else System.out.println("Conversation is too small to summarize!");
                break;
            case "/exit":
                return "/exit";
            default:
                System.out.println("'" + userMessage + "' is not a valid command");
                break;
        }
        if(!isPaste) {
            System.out.print("Write a message: ");
            userMessage = input.nextLine();
        }
        if (!userMessage.isEmpty() && userMessage.charAt(0) == '/') userMessage = commands(userMessage, tokens);
        return userMessage;
    }
    public static void chat(AiAgent agent) throws IOException, InterruptedException {
        Result reply = new Result("", 0);
        while (true) {
            input.reset();
            System.out.print("Write a message: ");
            String userMessage = input.nextLine();
            if (!userMessage.isEmpty() && userMessage.charAt(0) == '/') userMessage = commands(userMessage, reply.tokens());
            if (userMessage.equals("/exit")) break;
            long startTime = System.currentTimeMillis();
            long endTime = 0;
            System.out.print(agent.getName() + " is thinking...");
            messages.add(new ChatMessage("user", userMessage));
            reply = new Result("", 0);
            try {
                reply = agent.ask(messages, "default");
                endTime = System.currentTimeMillis() - startTime;
            } catch (NullPointerException e) {
                System.err.println("Trying a different model...");
                try {
                    Thread.sleep(20);
                    System.out.print(agent.getName() + " is thinking...");
                    startTime = System.currentTimeMillis();
                    reply = agent.ask(messages, "lite");
                    endTime = System.currentTimeMillis() - startTime;
                } catch (NullPointerException e1) {
                    System.err.println(e1.getMessage());
                }
            }
            if (!reply.text().isEmpty()) System.out.println("\n" + agent.getName() + ": " + reply.text() + "\n" +
                    "Thought for " + endTime/1000 + "[s], TTC: " + reply.tokens());
            if(summarize) {
                summarize = false;
                messages = new ArrayList<>();
            }
            messages.add(new ChatMessage("model", reply.text()));
        }
    }
    public static String paste() {
        System.out.print("Paste a message (add 'eof' at the end): ");
        input.useDelimiter("eof\n");
        return input.hasNext() ? input.next() : "";
    }
}
