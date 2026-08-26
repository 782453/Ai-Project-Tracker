package com.lior.tracker;

import java.nio.file.Path;

public final class AppPaths {
    public static final Path DATA = Path.of("data");
    public static final Path PROJECTS = DATA.resolve("projects.json");
    public static final Path HISTORY = DATA.resolve("chatHistory");
    public static final Path FILES = DATA.resolve("sendFiles");
}
