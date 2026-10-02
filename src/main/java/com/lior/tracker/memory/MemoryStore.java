package com.lior.tracker.memory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface MemoryStore {
    void save(MemoryEntry memory) throws IOException;
    List<MemoryEntry> loadAll() throws IOException;
    Optional<MemoryEntry> findById(String id) throws IOException;
    void delete(String id) throws IOException;
}
