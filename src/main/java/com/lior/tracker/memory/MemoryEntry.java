package com.lior.tracker.memory;

import java.time.LocalDateTime;
import java.util.List;

public record MemoryEntry(String id,
                          String title,
                          String content,
                          MemoryType type,
                          String project,
                          int importance,
                          /**1-3   = low importance
                           4-6   = useful
                           7-8   = important
                           9-10  = critical / should almost always be preserved*/
                          List<String> tags,
                          List<String> related,
                          LocalDateTime createdAt,
                          LocalDateTime updatedAt) {
    public MemoryEntry {
        if(importance < 1 || importance > 10) throw new IllegalArgumentException("Importance must be between 1 and 10");
    }
}

