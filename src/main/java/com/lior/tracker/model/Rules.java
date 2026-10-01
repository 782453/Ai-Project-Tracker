package com.lior.tracker.model;

import com.lior.tracker.AppPaths;
import com.lior.tracker.Chat;
import com.lior.tracker.ChatMessage;
import com.lior.tracker.agent.AiAgent;
import com.lior.tracker.agent.GeminiAgent;
import com.lior.tracker.agent.NvidiaAgent;
import com.lior.tracker.agent.Result;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Rules {
    private boolean maxTokens;
    private boolean autoSummarize;
    private boolean autoSave;
    private final String uuid = UUID.randomUUID().toString();
    public Rules() {
        System.out.print("maxTokens: ");
        setMaxTokens(Chat.getInput().nextBoolean());
        System.out.print("autoSummarize: ");
        setAutoSummarize(Chat.getInput().nextBoolean());
        System.out.print("autoSave: ");
        setAutoSave(Chat.getInput().nextBoolean());
        Chat.getInput().nextLine();
    }
    public Rules(boolean maxTokens, boolean autoSummarize, boolean autoSave) {
        this.maxTokens = maxTokens;
        this.autoSummarize = autoSummarize;
        this.autoSave = autoSave;
    }
    public void setMaxTokens(boolean maxTokens) {this.maxTokens = maxTokens;}
    public boolean isMaxTokens() {return this.maxTokens;}
    public void setAutoSummarize(boolean autoSummarize) {this.autoSummarize = autoSummarize;}
    public boolean isAutoSummarize() {return this.autoSummarize;}
    public void setAutoSave(boolean autoSave) {this.autoSave = autoSave;}
    public boolean isAutoSave() {return this.autoSave;}
    public String getUuid() {return this.uuid;}
    @Override
    public String toString() {return "Rules:\nmaxTokens: " + this.maxTokens + " - Minimize token usage\nautoSummarize: " + this.autoSummarize +
            " - Auto summarize chat\nautoSave: " + this.autoSave + " - Auto save chat";}
    public String getMaxPrompt() {
        return """
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
    }
    public static String getSummPrompt() {return """
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
                        """;}
    public static void autoSummarize(ChatSession session, Rules rules) throws IOException, InterruptedException {
        AiAgent agent = new GeminiAgent();
        if(Chat.getAgentName().equals("Nemotron")) agent = new NvidiaAgent();
        Result reply;
        session.getMessages().add(new ChatMessage("user", getSummPrompt()));
        try {
            reply = agent.ask(session.getMessages(), "default");
        } catch (RuntimeException e) {
            try {
                Thread.sleep(20);
                reply = agent.ask(session.getMessages(), "lite");
            } catch (RuntimeException e1) {
                return;
            }
        }
        if(reply.text().isEmpty()) return;
        session.getMessages().clear();
        if(rules.isMaxTokens()) {
            if(Chat.getAgentName().equals("Nemotron")) session.getMessages().add(new ChatMessage("system", rules.getMaxPrompt()));
            else {
                session.getMessages().add(new ChatMessage("user", rules.getMaxPrompt()));
                session.getMessages().add(new ChatMessage("model", "Memory updated!"));
            }
        }
        session.getMessages().add(new ChatMessage("user", "Your next message should be the summary of our chat"));
        session.getMessages().add(new ChatMessage("model", reply.text()));
    }
    public static void autoSaveChat(ChatSession session, String uuid) {
        if(session.getMessages().isEmpty()) return;
        List<ChatMessage> temp = new ArrayList<>();
        temp.addAll(session.getLastTwoMessage());
        uuid = "/" + uuid + ".txt";
        Path path = Path.of(AppPaths.AUTOSAVE + uuid);
        for(ChatMessage line : temp) {
            try {
                if(!Files.exists(path)) Files.createFile(path); //if the file doesn't exist it will create it
                Files.write(path, ("[" + line.getRole() + "]\n").getBytes(), StandardOpenOption.APPEND); //append to not override the file!
                Files.write(path, (line.getText() + "\n\n").getBytes(), StandardOpenOption.APPEND); //append to not override the file!
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
