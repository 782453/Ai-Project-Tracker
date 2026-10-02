package com.lior.tracker.memory;

import com.lior.tracker.AppPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MarkdownMemoryStore implements MemoryStore {
    @Override
    public void save(MemoryEntry memory) throws IOException {
        Files.createDirectories(AppPaths.MEMORY);
        Path filePath = AppPaths.MEMORY.resolve(sanitizedTitle(memory.title()) + "--" + memory.id() + ".md");

        Files.writeString(
                filePath,
                toMarkdown(memory),
                StandardCharsets.UTF_8
        );
    }
    @Override
    public List<MemoryEntry> loadAll() throws IOException {
        if(!Files.exists(AppPaths.MEMORY)) {
            System.out.println("Missing memory folder");
            return List.of();
        }
        List<Path> files;
        try (var stream = Files.list(AppPaths.MEMORY)) {
            files = stream.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".md")).toList();
        }
        if(files.isEmpty()) {
            System.out.println("No memory found!");
            return List.of();
        }
        List<MemoryEntry> entries = new ArrayList<>();
        for(Path file : files) {
            entries.add(fromMarkdown(file));
        }
        return entries;
    }
    @Override
    public Optional<MemoryEntry> findById(String id) throws IOException {
        if (!Files.exists(AppPaths.MEMORY)) return Optional.empty();
        Path p;
        try(var stream = Files.list((AppPaths.MEMORY))) {
            Optional<Path> file = stream.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith("--" + id + ".md")).findFirst();
            if(file.isEmpty()) return Optional.empty();
            p = file.get();
        }
        return Optional.of(fromMarkdown(p));
    }
    @Override
    public void delete(String id) throws IOException {
        if (!Files.exists(AppPaths.MEMORY)) return;
        Path p;
        try(var stream = Files.list((AppPaths.MEMORY))) {
            Optional<Path> file = stream.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith("--" + id + ".md")).findFirst();
            if(file.isEmpty()) return;
            p = file.get();
        }
        Files.deleteIfExists(p);
    }
    private String toMarkdown(MemoryEntry memory) {
        StringBuilder md = new StringBuilder();
        md.append("---\n");
        md.append("id: \"").append(memory.id()).append("\"\n");
        md.append("type: \"").append(memory.type()).append("\"\n");
        md.append("project: \"").append(memory.project()).append("\"\n");
        md.append("importance: ").append(memory.importance()).append("\n");
        md.append("created: \"").append(memory.createdAt()).append("\"\n");
        md.append("updated: \"").append(memory.updatedAt()).append("\"\n");
        md.append("tags:\n");
        for(String tag : memory.tags()) {
            md.append("  - ").append(tag).append("\n");
        }
        md.append("related:\n");
        for(String relate : memory.related()) {
            md.append("  - \"[[").append(relate).append("]]\"\n");
        }
        md.append("---\n\n");
        md.append("# ").append(memory.title()).append("\n\n");
        md.append(memory.content()).append("\n");
        return md.toString();
    }
    private MemoryEntry fromMarkdown(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        String id = null;
        MemoryType type = null;
        String project = null;
        int importance = 0;
        LocalDateTime created = null;
        LocalDateTime updated = null;
        List<String> tags = new ArrayList<>();
        List<String> related = new ArrayList<>();
        String title = null;
        StringBuilder content = new StringBuilder();
        boolean inFrontMatter = false;
        boolean frontMatterFinished = false;
        boolean titleFound = false;
        for(String line : lines) {
            if(line.equals("---")) {
                if (!inFrontMatter && !frontMatterFinished) inFrontMatter = true;
                else if (inFrontMatter) {
                    inFrontMatter = false;
                    frontMatterFinished = true;
                }
                continue;
            }
            if(inFrontMatter) {
                if (line.startsWith("id:")) id = line.substring(line.indexOf(":") + 1).trim().replace("\"", "");
                else if (line.startsWith("type:"))
                    type = MemoryType.valueOf(line.substring(line.indexOf(":") + 1).trim().replace("\"", ""));
                else if (line.startsWith("project:"))
                    project = line.substring(line.indexOf(":") + 1).trim().replace("\"", "");
                else if (line.startsWith("importance:"))
                    importance = Integer.parseInt(line.substring(line.indexOf(":") + 1).trim());
                else if (line.startsWith("created:"))
                    created = LocalDateTime.parse(line.substring(line.indexOf(":") + 1).trim().replace("\"", ""));
                else if (line.startsWith("updated:"))
                    updated = LocalDateTime.parse(line.substring(line.indexOf(":") + 1).trim().replace("\"", ""));
                else if (line.startsWith("  - \"[[")) related.add(line.substring(4).trim());
                else if (line.startsWith("  - ")) tags.add(line.substring(4).trim());
                continue;
            }
            if(frontMatterFinished && line.startsWith("# ")) {
                title = line.substring(2).trim().replace("\"", "");
                titleFound = true;
                continue;
            }
            if(titleFound) content.append(line).append("\n");
        }
        return new MemoryEntry(id, title, content.toString().trim(), type, project, importance, tags, related, created, updated);
    }
    private String sanitizedTitle(String title) {
        char[] chars = title.toCharArray();
        char[] invalidChars = {'<','>',':','"','/','\\','|','?','*'};
        for(int i = 0; i < chars.length; i++) {
            for(char c : invalidChars) {
                if(chars[i] == c) {
                    chars[i] = ' ';
                    break;
                }
            }
        }
        String sanitized = new String(chars).replaceAll("\\s+", " ").trim();
        if(sanitized.isBlank()) return "memory";
        return sanitized;
    }
}
