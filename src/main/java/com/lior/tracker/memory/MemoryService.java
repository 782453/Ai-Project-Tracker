package com.lior.tracker.memory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

public class MemoryService {
    private final MemoryStore store;
    public MemoryService(MemoryStore store) {
        this.store = store;
    }
    public MemoryEntry remember(String title, String content, MemoryType type, String project, int importance, List<String> tags, List<String> related) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        MemoryEntry memory = new MemoryEntry(UUID.randomUUID().toString(), title, content, type, project, importance, tags, related, now, now);
        this.store.save(memory);
        return memory;
    }
    public List<MemoryEntry> getAll()  throws IOException {
        return this.store.loadAll();
    }
    public Optional<MemoryEntry> getById(String id) throws IOException {
        return this.store.findById(id);
    }
    public void forget(String id) throws IOException {
        this.store.delete(id);
    }
    public List<MemoryEntry> search(String query, int limit) throws IOException {
        if(query == null || query.isBlank() || limit <= 0) return new ArrayList<>();
        char[] invalid = {'?', '!', '.', ',', ':'};
        for(char c : invalid) query = query.replace(c, ' ');
        query = query.toLowerCase(Locale.ROOT);
        Map<MemoryEntry, Integer> map = new HashMap<>();
        int temp;
        for(MemoryEntry memory : getAll()) {
            temp = 0;
            for(String str : query.split("\\s+")) {
                if (memory.title().toLowerCase(Locale.ROOT).contains(str)) {
                    temp += 5;
                }
                if (memory.content().toLowerCase(Locale.ROOT).contains(str)) {
                    temp += 1;
                }
                if (memory.project().toLowerCase(Locale.ROOT).contains(str)) {
                    temp += 3;
                }
                for (String tag : memory.tags()) {
                    if (tag.toLowerCase(Locale.ROOT).contains(str)) {
                        temp += 4;
                        break;
                    }
                }
            }
            if (temp > 0) map.put(memory, temp);
        }
        if(map.isEmpty()) {return new ArrayList<>();}
        List<MemoryEntry> search = new ArrayList<>();
        map.entrySet().stream().sorted(Map.Entry.<MemoryEntry, Integer>comparingByValue().reversed().thenComparing(
                Comparator.comparingInt((Map.Entry<MemoryEntry, Integer> e) -> e.getKey().importance()).reversed()
        )).forEach(entry -> search.add(entry.getKey()));
        List<MemoryEntry> ranked = new ArrayList<>();
        if(search.size() <= limit) return search;
        for(int i = 0; i < limit; i++) ranked.add(search.get(i));
        return ranked;
    }
    public void compressMemories() throws IOException {
        //TODO: think about the idea. right now it's looking for similar words in the title and content. maybe use search?
    }
}
