package com.lior.tracker;

import com.lior.tracker.model.ChatSession;
import com.lior.tracker.agent.*;
import com.lior.tracker.model.ProjectCommands;

import java.io.*;
//import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Chat {
    private static final Scanner input = new Scanner(System.in);
    private static List<ChatMessage> msgHistory = new ArrayList<>();
    private static AiAgent agent;
    private static String groqModel;
    private static boolean maximize = false;
    private static final String MAX_PROMPT = """
            I want to minimize token usage so in your replies:
             Remove:
             -repetition
             -greetings
             -filler:exclude transitional phrases, hedging, pleasantries
             -irrelevant parts
             1. **Format**:
                a. Headers: UPPERCASE labels.
                b. Lists: Numeric. No bullets.
                c. Code/JSON: Use ``` delimiters.
                d. Compression: Fragments, remove articles/prepositions if unambiguous.
             2. **Style**:
                a. Verbs: Imperative.
                b. Tone: Direct. Zero hedging/pleasantries.
                c. Banned: Repetition, greetings, filler, irrelevant content.
             3. **Content**:
                a. Explanations: Ban. State facts only.
                b. Abbreviations: Define early (domain-specific ISO/W3C), reuse later.
                c. Context: Minimal. If missing, output "MISSING: [key]".
                d. Ambiguity Check: If fragment unclear, append max 5 word clarification.
                e. Exceptions: Safety/Critical failures override compression.
                f. Scope: Single response. One conclusion.
            """;
    public static void main(String[] args) throws IOException, InterruptedException, NullPointerException {
        System.out.println("Ai models list:\n[0] Gemini\n[1] ChatGPT\n[2] Qwen\n[3] Nemotron");
        System.out.print("Choose Ai model: ");
        int num;
        try {
            num = Integer.parseInt(input.nextLine());
        }catch(NumberFormatException e) {
            System.err.println("[ERROR] Please enter a number!");
            return;
        }
        switch(num) {
            case 0:
                agent = new GeminiAgent();
                System.out.println("Model selected: Gemini");
                break;
            case 1:
                agent = new GroqAgent();
                groqModel = "openai/gpt-oss-120b";
                System.out.println("Model selected: ChatGPT");
                break;
            case 2:
                agent = new GroqAgent();
                groqModel = "qwen/qwen3.8-27b";
                System.out.println("Model selected: Qwen");
                break;
            case 3:
                agent = new NvidiaAgent();
                System.out.println("Model selected: Nvidia");
            default:
                System.err.println("Invalid choice!");
                return;
        }
        System.out.print("Maximize token usage (y/n)? ");
        if (input.nextLine().equalsIgnoreCase("y")) maximize = true;
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
    /*
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
     */
    public static void chat(AiAgent agent) throws IOException, InterruptedException {
        Result reply = new Result("", 0);
        ChatSession session = new ChatSession(new ArrayList<>(), new ArrayList<>(), reply.text(), reply.tokens());
        while (true) {
            if(session.getMessages().size() == 0 && maximize) {
                session.getMessages().add(new ChatMessage("user", MAX_PROMPT));
                session.getMessages().add(new ChatMessage("model", "OK!"));
            }
            input.reset();
            System.out.print("Write a message: ");
            session.setUserMessage(input.nextLine());
            if (!session.getUserMessage().isEmpty() && session.getUserMessage().charAt(0) == '/') session = ProjectCommands.commands(session);
            if (session.getUserMessage().equals("/exit")) break;
            long startTime = System.currentTimeMillis();
            long endTime = 0;
            System.out.println(agent.getName() + " is thinking...");
            if (!session.getUserMessage().equals("/send")) session.getMessages().add(new ChatMessage("user", session.getUserMessage()));
            reply = new Result("", 0);
            try {
                reply = agent.ask(session.getMessages(), "default");
                endTime = System.currentTimeMillis() - startTime;
            } catch (RuntimeException e) {
                System.err.println(e.getMessage() + "\nTrying a different model...");
                try {
                    Thread.sleep(20);
                    System.out.println(agent.getName() + " is thinking...");
                    startTime = System.currentTimeMillis();
                    reply = agent.ask(session.getMessages(), "lite");
                    endTime = System.currentTimeMillis() - startTime;
                } catch (RuntimeException e1) {
                    System.err.println(e1.getMessage());
                }
            }
            if (!reply.text().isEmpty()) System.out.println("\n" + agent.getName() + ": " + reply.text() + "\n" +
                        "Thought for " + endTime/1000 + "[s], TTC: " + reply.tokens());
            if(ProjectCommands.isSummarize()) {
                ProjectCommands.setSummarize(false);
                session.getMessages().clear();
                session.getMessages().add(new ChatMessage("user", "Your next message should be the summary of our chat"));
            }
            session.getMessages().add(new ChatMessage("model", reply.text()));
            session.setTokens(reply.tokens());
            if(!ProjectCommands.isLoad()) History(session.getLastTwoMessage());
            else ProjectCommands.setLoad(false);
        }
    }
    public static Scanner getInput() {return input;}
    public static void History(List<ChatMessage> messages) {
        for (ChatMessage msg : messages) {
            if(msg.getRole().equals("user") && msg.getInline_data() != null) msgHistory.add(
                    new ChatMessage("user", msg.getText() + "\nA file was attached here: " + msg.getFilename()));
            else msgHistory.add(msg);
        }
    }
    public static List<ChatMessage> getHistory() {return msgHistory;}
    public static void setHistory(List<ChatMessage> history) {msgHistory = history;}
    public static void resetHistory() {msgHistory.clear();}
    public static String getGroqModel() {return groqModel;}
    public static boolean isMaximize() {return maximize;}
    public static String getMaxPrompt() {return MAX_PROMPT;}
}
