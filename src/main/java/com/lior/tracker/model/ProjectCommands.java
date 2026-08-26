package com.lior.tracker.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lior.tracker.AppPaths;
import com.lior.tracker.Chat;
import com.lior.tracker.ChatMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

public class ProjectCommands {
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
                        -save the most important parts of a code blocks, remember method names, variables, and what they do
                        Remove:
                        -repetition
                        -greetings
                        -filler
                        -irrelevant conversation
                        Important:
                        -Do not invent information!
                        -Reply only with the summarization, nothing else!
                        -Maximize the summary to minimize token consumption in the future!
                        """;
    private static boolean load = false;
    private static boolean summarize = false;
    public static ChatSession commands(ChatSession session) throws IOException {
        Scanner input = Chat.getInput();
        int num = -1;
        boolean isPaste = false;
        switch (session.getUserMessage()) {
            case "/?":
                System.out.println("""
                        \tCommands:
                        \t/nchat - Start a fresh chat
                        \t/nproject - Create a new project
                        \t/projects - Project options
                        \t/sprojects - Saves projects
                        \t/send - Send a file
                        \t/paste - Send large text (Ctrl + v)
                        \t/summarize - Summarize the chat
                        \t/save - Save chat history
                        \t/load - Load chat history
                        \t/exit - Exit the program""");
                break;
            case "/nchat":
                load = false;
                session.setMessages(new ArrayList<>());
                if(Chat.isMaximize()) {
                    session.getMessages().add(new ChatMessage("user", Chat.getMaxPrompt()));
                    session.getMessages().add(new ChatMessage("model", "OK!"));
                }
                if (!Chat.getHistory().isEmpty()) {
                    System.out.print("Save chat history (y/n)? ");
                    if (input.nextLine().equalsIgnoreCase("y")) saveHistory(Chat.getHistory());
                }
                Chat.resetHistory();
                break;
            case "/nproject":
                if(Files.exists(Path.of("projects.json"))) session.setProjects(Project.getProjects(false));
                System.out.print("Project name: ");
                String name = input.nextLine();
                System.out.print("Project status (ACTIVE, BLOCKED, PAUSED, DONE): ");
                String status = input.nextLine();
                System.out.print("Last updated (YYYY-MM-DD): ");
                String lastUpdated = input.nextLine();
                System.out.print("Project notes: ");
                String notes = input.nextLine();
                session.getProjects().add(new Project(name, status, lastUpdated, notes));
                System.out.println("Project added!");
                saveProjects(session.getProjects());
                break;
            case "/projects":
                if(!Files.exists(Path.of("projects.json"))) {
                    System.out.println("[ERROR] No projects file found");
                    break;
                }
                if(session.getProjects().isEmpty()) session.setProjects(Project.getProjects(true));
                else for (int i = 0; i < session.getProjects().size(); i++) System.out.println("[" + i + "] " + session.getProjects().get(i).getName() +
                        " - " + session.getProjects().get(i).getStatus());
                System.out.print("Select a project: ");
                try {
                    num = Integer.parseInt(input.nextLine());
                }catch(NumberFormatException e) {
                    System.err.println("[ERROR] Please enter a number!");
                }
                if(num == -1) {System.out.println(); break;}
                if(num >=0 && num < session.getProjects().size()) {
                    System.out.println("Selected project: " + session.getProjects().get(num).getName());
                    System.out.print("[w] Work on project, [c] Change project status, [a] Add notes, [DEL] Delete project\nOption: ");
                    //input.nextLine();
                    String proj = input.nextLine();
                    switch (proj) {
                        case "w":
                            session.getMessages().add(new ChatMessage("user", "We'll continue working on this project today: " + session.getProjects().get(num).getName() +
                                    "\nNotes: " + session.getProjects().get(num).getNotes()));
                            session.getMessages().add(new ChatMessage("model", "OK!"));
                            System.out.println("Currently working on: " + session.getProjects().get(num).getName());
                            break;
                        case "c":
                            System.out.print("Select status (ACTIVE, BLOCKED, PAUSED, DONE): ");
                            session.getProjects().get(num).setStatus(input.nextLine());
                            System.out.println("Project status changed!");
                            saveProjects(session.getProjects());
                            break;
                        case "a":
                            System.out.print("Write notes to add: ");
                            session.getProjects().get(num).addNotes(input.nextLine());
                            System.out.println("Notes added!");
                            saveProjects(session.getProjects());
                            break;
                        case "DEL":
                            session.getProjects().remove(num);
                            System.out.println("Project removed!");
                            saveProjects(session.getProjects());
                            break;
                        default:
                            System.out.println("Invalid choice");
                            break;
                    }
                } else System.out.println("Invalid choice.");
                break;
            case "/sprojects":
                saveProjects(session.getProjects());
                break;
            case "/send":
                sendFile(session);
                return session;
            case "/paste":
                session.setUserMessage(paste(input));
                input.nextLine();
                isPaste = true;
                break;
            case "/summarize":
                if(session.getTokens() >= 1000) {
                    summarize = true;
                    session.setUserMessage(SUMM_PROMPT);
                    return session;
                } else System.out.println("Conversation is too small to summarize!");
                break;
            case "/save":
                saveHistory(Chat.getHistory());
                break;
            case "/load":
                load = false;
                loadHistory();
                session.setMessages(Chat.getHistory());
                break;
            case "/exit":
                session.setUserMessage("/exit");
                return session;
            default:
                System.out.println("'" + session.getUserMessage() + "' is not a valid command");
                break;
        }
        if(!isPaste) {
            System.out.print("Write a message: ");
            session.setUserMessage(input.nextLine());
        }
        if (!session.getUserMessage().isEmpty() && session.getUserMessage().charAt(0) == '/') session.setUserMessage(commands(session).getUserMessage());
        return session;
    }
    public static String paste(Scanner input) {
        System.out.print("Paste a message (add 'eof' at the end): ");
        input.useDelimiter("eof\n");
        return input.hasNext() ? input.next() : "";
    }
    public static void saveProjects(List<Project> projects) throws IOException {
        if(projects.isEmpty()) projects.add(new Project("", "" + Project.Status.BLOCKED, "0000-00-00", "Just add a project to save..."));
        ObjectMapper mapper = new ObjectMapper();
        Path path = Path.of("projects.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), projects);
        System.out.println("Projects saved!");
    }
    public static void sendFile(ChatSession session) throws IOException {
        Files.createDirectories(Path.of("sendFiles"));
        System.out.print("Enter file name: ");
        String file = Chat.getInput().nextLine();
        Path path = Path.of("sendFiles", file);
        if(!Files.exists(path)) {
            System.out.println("File does not exist(make sure it's in files folder)!");
            session.setUserMessage("error. File does not exist!");
            return;
        }
        String fileBase64 = Base64.getEncoder().encodeToString(Files.readAllBytes(path));
        System.out.print("File selected: " + file + "\nWrite a message: ");
        session.getMessages().add(new ChatMessage("user", Chat.getInput().nextLine(), fileBase64, Files.probeContentType(path), file));
        session.setUserMessage("/send");
    }
    public static void saveHistory(List<ChatMessage> msgHistory) throws IOException {
        if(msgHistory.isEmpty()) {
            System.out.println("No previous messages found!");
            return;
        }
        Files.createDirectories(Path.of("chatHistory"));
        System.out.print("Enter chat name: ");
        String chatName = Chat.getInput().nextLine() + "-" + LocalDate.now().toString() + "-" + UUID.randomUUID().toString();
        Path path = Path.of("chatHistory", chatName + ".json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), msgHistory);
        System.out.println("History saved as: " + chatName + ".json");
    }
    public static void loadHistory() throws IOException {
        if(!Files.exists(Path.of("chatHistory"))) {
            System.out.println("Missing chatHistory folder!");
            return;
        }
        int num = -1;
        Chat.resetHistory();
        Path path = Path.of("chatHistory\\");
        try (var stream = Files.list(path)) {
            if(stream.findAny().isEmpty()) {
                System.out.println("No chat history files found!");
                return;
            }
        }
        List<Path> files;
        int i = 0;
        try (var stream = Files.list(path)) {
            System.out.println("Previous chats:");
            files = stream.filter(Files::isRegularFile).toList();
        }
        for(Path file : files) {
            System.out.println("[" + i + "] " + file.getFileName().toString());
            i++;
        }
        System.out.print("Select a chat to load: ");
        try {
            num = Integer.parseInt(Chat.getInput().nextLine());
        }catch(NumberFormatException e) {
            System.err.println("[ERROR] Please enter a number!");
        }
        if(num == -1) {System.out.println(); return;}
        if(num >= 0 && num < files.size()) {
            ObjectMapper mapper = new ObjectMapper();
            Chat.setHistory(mapper.readValue(files.get(num).toFile(), new TypeReference<List<ChatMessage>>() {}));
            load = true;
        }else System.out.println("Invalid choise!");
    }
    public static boolean isLoad() {return load;}
    public static void setLoad(boolean load_) {load = load_;}
    public static boolean isSummarize() {return summarize;}
    public static void setSummarize(boolean summarize_) {summarize = summarize_;}
}
