package com.lior.tracker.memory;

import java.util.List;

public record MemoryCandidate(String title, String content, MemoryType type, String project, int importance, List<String> tags, List<String> related) {
}
