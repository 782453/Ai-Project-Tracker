# AI Project Tracker

A Java command-line AI workspace for chatting with multiple AI providers while keeping project notes, conversation history, file attachments, token-aware summaries, and session rules in one place.

The application currently supports **Google Gemini**, **Groq-hosted GPT-OSS and Qwen models**, and **NVIDIA Nemotron** through a shared `AiAgent` interface. Groq and NVIDIA responses can be streamed directly to the console.
> **Project status:** active development. The CLI is the primary interface. A Swing history viewer is available, while the full `ChatWindow` GUI is not implemented yet.

---
## Features
- Multi-provider AI chat through one common interface
- Gemini, GPT-OSS, Qwen, and Nemotron model options
- Streaming output for Groq and NVIDIA responses
- Token usage tracking from provider responses
- Manual and automatic conversation summarization
- Project tracking with status and notes
- File attachments using Base64 encoding
- Saved chat history with reload support
- Automatic plain-text chat autosaves
- Configurable per-session rules
- Read-only Swing windows for current and full conversation history
- Local runtime data separated under the `data/` directory
- API keys loaded from environment variables rather than source code
- Persistent long-term memory stored as local Markdown files
- Automatic retrieval of relevant memories before provider requests
- AI-assisted memory extraction after successful exchanges, with explicit approval before saving
- Manual memory creation through `/memory add`
- Obsidian-compatible memory files with YAML front matter and `[[wiki links]]`

---

## Tech Stack

| Component | Technology |
|---|---|
| Language | Java 25 |
| Build tool | Maven |
| JSON | Jackson Databind 2.22.1 |
| HTTP | Java `HttpClient` |
| Desktop UI | Swing |
| Persistence | Local JSON / text / Markdown files |
| Memory storage | Markdown with YAML front matter |
| AI APIs | Google Gemini, Groq, NVIDIA NIM |

No provider SDK is required. API requests are built and sent directly with Java's standard HTTP client.

---

## Supported AI Models

The current model selection in `Chat.java` is:

| CLI option | Display name | Provider | Model |
|---:|---|---|---|
| `0` | Gemini | Google Gemini API | `gemini-3.5-flash` |
| fallback | Gemini Lite | Google Gemini API | `gemini-3.5-flash-lite` |
| `1` | ChatGPT | Groq | `openai/gpt-oss-120b` |
| `2` | Qwen | Groq | `qwen/qwen3.8-27b` |
| `3` | Nemotron | NVIDIA NIM | `nvidia/nemotron-3-ultra-550b-a55b` |
> **Note:** option `1` is currently displayed as `ChatGPT` in the CLI, but the application is not calling the ChatGPT product or OpenAI API. It uses the open-weight `openai/gpt-oss-120b` model through Groq.

Model IDs are currently configured directly in source code and may need to be updated if a provider changes model availability.

---

## Requirements

- **JDK 25**
- **Apache Maven**
- Internet connection
- At least one API key for the provider you want to use

The Maven project currently targets Java 25:

```xml
<maven.compiler.source>25</maven.compiler.source>
<maven.compiler.target>25</maven.compiler.target>
```

---

## API Keys

The application reads provider credentials from environment variables.

| Provider | Environment variable |
|---|---|
| Gemini | `GEMINI_API_KEY` |
| Groq | `GROQ_API_KEY` |
| NVIDIA | `NVIDIA_API_KEY` |
### Windows PowerShell

For the current terminal session:

```powershell
$env:GEMINI_API_KEY="your-key"
$env:GROQ_API_KEY="your-key"
$env:NVIDIA_API_KEY="your-key"
```

For persistent Windows environment variables:

```powershell
setx GEMINI_API_KEY "your-key"
setx GROQ_API_KEY "your-key"
setx NVIDIA_API_KEY "your-key"
```

Open a new terminal after using `setx`.
### macOS / Linux

```bash
export GEMINI_API_KEY="your-key"
export GROQ_API_KEY="your-key"
export NVIDIA_API_KEY="your-key"
```

You only need the key for the provider you intend to use, except when using file attachments with Groq or NVIDIA. Those attachment paths currently use Gemini as an intermediate file-processing step and therefore also require `GEMINI_API_KEY`.

> Never commit real API keys to GitHub. Keep them in environment variables or another local secrets mechanism.

---

## Installation

Clone the repository:

```bash
git clone https://github.com/782453/ai-project-tracker.git
cd ai-project-tracker
```

Compile/package with Maven:

```bash
mvn clean package
```

The application entry point is:

```text
com.lior.tracker.Chat
```
### IntelliJ IDEA

The simplest development workflow is:

1. Open the repository as a Maven project.
2. Set the Project SDK to **JDK 25**.
3. Let Maven resolve Jackson.
4. Open `src/main/java/com/lior/tracker/Chat.java`.
5. Run `Chat.main()`.

The current `pom.xml` does not configure a shaded/fat JAR, so running directly from the IDE is the easiest option during development.

---

## First Run

At startup the application creates its local runtime directories if they do not already exist:

```text
data/
├── projects.json
├── chatHistory/
├── sendFiles/
├── autoSave/
└── memory/
```

It then displays the model selector:

```text
AI-Project-Tracker
Ai models list:
[0] Gemini
[1] ChatGPT
[2] Qwen
[3] Nemotron
Choose Ai model:
```

After a model is selected, a default session starts with all optional session rules disabled.
When Nemotron is selected, the application also offers the option to add an initial system instruction before the conversation begins.

The new `data/memory/` directory is the long-term memory vault. Each memory is stored as an individual Markdown file and can also be opened directly as an Obsidian vault.

---

## Commands

Enter:

```text
/?
```

to display the first command page.
### Page 1
| Command | Description |
|---|---|
| `/nchat` | Start a fresh chat session |
| `/nproject` | Create a new tracked project |
| `/projects` | List projects and open project actions |
| `/sprojects` | Save the current project list |
| `/send` | Attach and send a file from `data/sendFiles/` |
| `/paste` | Paste a large multi-line message using `eof` as the delimiter |
| `/summarize` | Summarize the current conversation context |
| `/save` | Save the accumulated chat history |
| `/load` | Load a previously saved chat |
| `/++` | Open command page 2 |
### Page 2

| Command | Description |
|---|---|
| `/history` | Open the active AI context in a Swing history window |
| `/fhistory` | Open the full accumulated session history |
| `/rules` | View or change session rules |
| `/resend` | Resend the most recent pending user message |
| `/memory add` | Save a memory manually |
| `/memory comp` | Call the memory-compression hook; compression is currently not implemented |
| `/exit` | Exit the application |
| `/..` | Return to command page 1 |

---

## Long-Term Memory

The current memory system gives the AI persistent, local context without permanently resending the complete conversation.

Memory is stored as human-readable Markdown under:

```text
data/memory/
```

Each saved memory is its own `.md` file.

### Memory flow

For a normal successful chat turn, the application currently does the following:

1. Reads the user's message.
2. Searches the local memory vault for relevant memories.
3. Selects up to five matching entries.
4. Temporarily appends those memories to the user prompt under `[Relevant memories]`.
5. Sends the enriched conversation to the selected AI provider.
6. Removes the injected memory block from the active chat context and restores the original user message.
7. Stores the assistant response normally.
8. Sends the latest user/assistant exchange to `MemoryExtractor`.
9. The extractor decides whether the exchange introduced something worth remembering.
10. If a candidate is returned, the CLI asks:

```text
Save memory (y/n)?
```

11. Only when the user answers `y` is the memory written to disk.

This means recalled memory can influence the current answer without becoming permanent duplicate chat history.

### What can become a memory?

`MemoryExtractor` currently asks the model to consider:

- Explicit requests to remember something
- Important facts
- Preferences
- Decisions
- Technical information
- Project information
- Persistent constraints

Temporary or unimportant conversation should be ignored.

The current memory types are:

```text
FACT
PREFERENCE
DECISION
TECHNICAL
PROJECT
PERSON
GENERAL
```

### Memory fields

A `MemoryEntry` contains:

```text
id
title
content
type
project
importance
tags
related
createdAt
updatedAt
```

`importance` must be between `1` and `10`.

The code describes the intended importance scale as:

| Importance | Meaning |
| --- | --- |
| `1-3` | Low importance |
| `4-6` | Useful |
| `7-8` | Important |
| `9-10` | Critical / should almost always be preserved |

### Memory file format

Files are written with a sanitized title plus their UUID:

```text
<sanitized-title>--<uuid>.md
```

A typical memory looks like:

```markdown
---
id: "1effa262-b820-4b00-a2e2-69b234ead3b3"
type: "PROJECT"
project: "ai project tracker"
importance: 8
created: "2026-10-02T12:23:50.471756400"
updated: "2026-10-02T12:23:50.471756400"
tags:
  - java
  - memory
  - obsidian
related:
  - "[[PROJECT]]"
  - "[[java]]"
---

# Project Context - AI Project Tracker

Important long-term context about the project goes here.
```

Because the files are plain Markdown, they are readable and editable without the application.

### View the memory vault in Obsidian

The memory folder can be opened directly as an Obsidian vault.

1. Open Obsidian.
2. Choose the option to open a folder as a vault.
3. Select:

```text
<Ai-Project-Tracker>/data/memory/
```

Each stored memory becomes an Obsidian note.

The `related` property is written using Obsidian-style `[[wiki links]]`, allowing related notes/topics to appear as connections in Obsidian when those links resolve.

Because `data/` is ignored by Git, the Obsidian memory vault stays local unless you back it up or sync it separately.

### Manual memory creation

Use:

```text
/memory add
```

The CLI asks for:

- Title
- Multi-line content, ending with `eof`
- Memory type
- Project name
- Importance from `1-10`
- Tags
- Related tags/topics

The memory is then saved through the same `MemoryService` and `MarkdownMemoryStore` used by automatically extracted memories.

### Memory search and ranking

Memory recall is currently local lexical search rather than embeddings/vector search.

The query is lowercased and basic punctuation is stripped. Each query term contributes to a memory's score when it appears in:

| Field | Score per matching query term |
| --- | ---: |
| Title | `+5` |
| Tag | `+4` |
| Project | `+3` |
| Content | `+1` |

Matches are sorted by score, with memory importance used as a tie-breaker.

`Chat` requests at most five memories for the current user message.

### Current memory limitations

- Retrieval is keyword-based, not semantic/vector search.
- `related` links are metadata/Obsidian relationships; they are not part of retrieval ranking.
- `updatedAt` exists in the model, but there is no memory-update workflow yet.
- `MemoryService` contains `getAll`, `getById`, and `forget`, but there are no CLI commands for browsing/deleting memories yet.
- `/memory comp` exists, but `compressMemories()` is currently a TODO and performs no compression.
- Automatic memory extraction makes an additional AI request after a successful response.
- The token count stored in the active session represents the main provider response; the extractor's separate request is not added to that session token count.

---

## Project Tracking

Projects are represented by the `Project` class and stored in:

```text
data/projects.json
```

Each project contains:

```text
name
status
lastUpdated
notes
```

Available statuses are:

```text
ACTIVE
BLOCKED
PAUSED
DONE
```

### Creating a project

Use:

```text
/nproject
```

The application asks for the project name, status, last-updated date, and notes, then saves the updated project list.
### Working with an existing project

Use:

```text
/projects
```

After selecting a project, the current actions are:

| Option | Action |
|---|---|
| `w` | Add the project name and notes to the AI conversation context |
| `c` | Change the project status |
| `a` | Append project notes |
| `DEL` | Delete the selected project |

Selecting `w` lets the AI continue the conversation with the selected project's stored notes already in context.

---

## Chat Messages

All providers share the same internal `ChatMessage` model.

A message can contain:

```text
role
text
inline_data
mimeType
filename
```

The file-related fields are optional and are populated only for attachment messages.

Typical roles are:

```text
user
model
system
```

`MessagesMapper` converts these shared messages into the JSON format required by each provider.

For Groq/NVIDIA-compatible requests, the internal `model` role is converted to `assistant`.

---

## File Attachments

Files to be sent are placed in:

```text
data/sendFiles/
```

Then use:

```text
/send
```

The application:

1. Reads the selected file.
2. Encodes it as Base64.
3. Detects its MIME type with `Files.probeContentType`.
4. Stores the encoded data, MIME type, and filename in a `ChatMessage`.

### Gemini

Gemini receives the text and file as separate message parts using Gemini's `inline_data` structure.
### Groq and NVIDIA

Groq and NVIDIA chat requests are text-based in the current implementation. When the latest message contains a file, the application first sends that message to Gemini, adds Gemini's file interpretation back into the conversation, and then continues with the selected Groq or NVIDIA model.

Because of this bridge, using attachments with Groq or NVIDIA also requires a valid `GEMINI_API_KEY`.

---

## Streaming

Streaming is currently enabled for:

- Groq
- NVIDIA Nemotron

`GroqAgent` and `NvidiaAgent` read the Server-Sent Events response line by line, print each content chunk as it arrives, and collect the chunks into the final `Result` returned to the main chat loop.

Both request builders enable:

```json
"stream": true,
"stream_options": {
  "include_usage": true
}
```

This allows the application to retrieve the final token usage while still displaying the answer progressively.
Gemini currently uses a normal non-streaming response.

---

## Token Tracking

Each provider returns a common:

```java
public record Result(String text, int tokens) {}
```

The `tokens` field stores the provider-reported total token count for the request.

After a completed response the CLI prints the approximate request duration and total token count (`TTC`).

Token usage is also used by the summarization system.

The memory extractor makes a separate AI request after a successful chat response. That extractor request is not currently added to `ChatSession.tokens`, so the displayed/session token value reflects the main provider response rather than the total cost of both calls.

---

## Conversation Summarization

Long conversations resend an increasingly large message list to the provider. AI Project Tracker includes both manual and automatic summarization to reduce that context size.
### Manual summarization

Use:

```text
/summarize
```

Manual summarization is available when the current provider-reported token count is at least **1000**.

The application asks the AI to preserve important information such as:

- facts and user preferences
- decisions already made
- technical details
- class, method, variable, API, and model names
- constraints
- unresolved tasks and questions
- important code details
- recent conversation state
After the summary is returned, the active context is replaced with a much smaller summary-based conversation.
### Automatic summarization

When the `autoSummarize` rule is enabled, the application automatically summarizes the conversation once the session token count reaches **10,000** or more.

Gemini is used for automatic summarization by default. Nemotron sessions use `NvidiaAgent` instead.

---

## Session Rules

Use:

```text
/rules
```

to inspect or edit the current rules.

The current `Rules` class contains three settings:

| Rule | Purpose |
|---|---|
| `maxTokens` | Inject a compact-response instruction designed to minimize unnecessary output tokens |
| `autoSummarize` | Automatically summarize the active context at 10,000+ tokens |
| `autoSave` | Automatically append conversation turns to a local text file |

A new application session starts with all three disabled:
```text
maxTokens: false
autoSummarize: false
autoSave: false
```
### `maxTokens`

When enabled, a prompt is added that asks the model to remove repetition, greetings, filler, and irrelevant content and to answer in a compact format.

For Nemotron this is sent as a `system` message. For the other current providers it is inserted into the conversation as a user/model exchange.
### `autoSave`

Each `Rules` object receives a UUID. When autosave is enabled, conversation turns are appended to:

```text
data/autoSave/<uuid>.txt
```

Autosaves are plain text and separate from manually saved JSON chat histories.

---

## Chat History

The application maintains two related forms of history.
### Active context

`ChatSession.messages` contains the messages currently being sent back to the selected AI provider.

This list may become smaller after summarization or when a new chat is started.

### Full session history

`Chat.msgHistory` accumulates user/model turns separately from the active context.

For attached files, the full history stores a readable filename marker instead of duplicating the Base64 payload.
### History viewer

`HistoryWindow` provides a read-only Swing view for either list:

```text
/history   -> active context
/fhistory  -> full accumulated history
```

Messages are formatted as `YOU`, the current AI name, or `SYSTEM`.

### Saving history

Use:

```text
/save
```

Saved conversations are written as JSON to:

```text
data/chatHistory/
```

Filenames include the chosen chat name, the current date, and a UUID.
### Loading history

Use:

```text
/load
```

The application lists saved JSON conversations and lets you choose one to restore into the active context.

---

## Architecture

The application uses a small provider abstraction so the main chat loop does not need separate logic for each API. The current memory subsystem plugs into the same main chat loop before and after the provider call.

```mermaid
graph TD
    Chat[Chat / CLI] --> Session[ChatSession]
    Chat --> Commands[ProjectCommands]
    Commands --> Commands2[ProjectCommands2]
    Commands --> Projects[Project]
    Commands --> Rules[Rules]
    Commands2 --> History[HistoryWindow]

    Chat --> MemoryService[MemoryService]
    Chat --> Extractor[MemoryExtractor]
    MemoryService --> MemoryStore[MemoryStore]
    MemoryStore --> MarkdownStore[MarkdownMemoryStore]
    MarkdownStore --> MemoryFiles[data/memory/*.md]

    Chat --> Agent[AiAgent]
    Extractor --> Agent
    Agent --> Gemini[GeminiAgent]
    Agent --> Groq[GroqAgent]
    Agent --> Nvidia[NvidiaAgent]

    Gemini --> Mapper[MessagesMapper]
    Groq --> Mapper
    Nvidia --> Mapper

    Mapper --> GeminiAPI[Gemini API]
    Mapper --> GroqAPI[Groq API]
    Mapper --> NvidiaAPI[NVIDIA NIM API]
```
### `AiAgent`

All providers implement:

```java
public interface AiAgent {
    Result ask(List<ChatMessage> messages, String ver)
            throws IOException, InterruptedException;

    String getName();
    boolean isStream();
}
```

This makes provider selection independent from most of the main chat workflow.
### `Chat`

Main application entry point and conversation loop.

Responsibilities include:

- startup/data-directory initialization
- model selection
- active provider selection
- reading user input
- command dispatch
- provider calls and retry behavior
- response timing
- token display
- full-history accumulation
- automatic summarization/autosave triggers
- searching for relevant long-term memories before a provider request
- temporarily injecting recalled memories into the latest user message
- restoring the original user message after the provider call
- running `MemoryExtractor` after a successful exchange
- asking the user before a new extracted memory is persisted

### `ChatSession`

Holds the mutable state of the active conversation:

```text
projects
messages
userMessage
tokens
memories
```
### `MessagesMapper`

Builds provider-specific JSON request bodies.

It currently supports:

- Gemini `contents` / `parts`
- Gemini Base64 `inline_data`
- Groq OpenAI-compatible `messages`
- NVIDIA OpenAI-compatible `messages`
- streaming configuration and usage reporting
- Nemotron generation parameters

### `GeminiAgent`

Handles:

- `GEMINI_API_KEY`
- Gemini request construction
- default/lite model selection
- HTTP requests
- response parsing
- token extraction
### `GroqAgent`

Handles:

- `GROQ_API_KEY`
- GPT-OSS / Qwen requests through Groq
- streamed response parsing
- token extraction from streaming usage data
- Gemini-assisted file preprocessing

### `NvidiaAgent`

Handles:

- `NVIDIA_API_KEY`
- Nemotron requests through NVIDIA NIM
- streamed response parsing
- token extraction from streaming usage data
- Nemotron generation configuration
- optional system instructions
- Gemini-assisted file preprocessing
### `ProjectCommands` / `ProjectCommands2`

Implement the two-page slash-command system, including project operations, file sending, history management, summarization, rules, memory commands, and navigation.

### `Rules`

Stores the current optional session behavior and implements automatic summarization and autosave.

### `HistoryWindow`

A Swing `JFrame` containing a read-only `JTextArea` for inspecting conversation history without interrupting the CLI.
### `ChatWindow`

Currently reserved for the future full Swing chat interface. The class exists but does not yet contain an implementation.

### `MemoryService`

Provides the memory-domain operations used by the chat loop and manual memory command:

- `remember(...)`
- `getAll()`
- `getById(...)`
- `forget(...)`
- `search(...)`
- `compressMemories()` (currently a TODO/stub)

The current search implementation is local keyword scoring rather than embeddings/vector search.

### `MemoryStore`

Defines the persistence abstraction for memory storage: save, load all, find by ID, and delete by ID.

### `MarkdownMemoryStore`

Implements `MemoryStore` using individual Markdown files under `data/memory/`. It serializes memory metadata as YAML-style front matter and writes relationships as Obsidian-style `[[wiki links]]`.

### `MemoryExtractor`

Analyzes only the latest user/assistant exchange, asks the active `AiAgent` whether anything is worth storing long-term, parses the returned JSON into a `MemoryCandidate`, and leaves the final save decision to the user.

### `MemoryEntry` / `MemoryCandidate` / `MemoryType`

`MemoryEntry` is the persisted long-term memory record, `MemoryCandidate` represents an extracted but not-yet-approved memory, and `MemoryType` defines the supported categories.

---

## Project Structure
```text
ai-project-tracker/
├── .gitignore
├── LICENSE
├── README.md
├── pom.xml
│
├── src/
│   ├── main/
│   │   ├── java/com/lior/tracker/
│   │   │   ├── AppPaths.java
│   │   │   ├── Chat.java
│   │   │   ├── ChatMessage.java
│   │   │   │
│   │   │   ├── agent/
│   │   │   │   ├── AiAgent.java
│   │   │   │   ├── GeminiAgent.java
│   │   │   │   ├── GroqAgent.java
│   │   │   │   ├── NvidiaAgent.java
│   │   │   │   └── Result.java
│   │   │   │
│   │   │   ├── gui/
│   │   │   │   ├── ChatWindow.java
│   │   │   │   └── HistoryWindow.java
│   │   │   │
│   │   │   ├── memory/
│   │   │   │   ├── MarkdownMemoryStore.java
│   │   │   │   ├── MemoryCandidate.java
│   │   │   │   ├── MemoryEntry.java
│   │   │   │   ├── MemoryExtractor.java
│   │   │   │   ├── MemoryService.java
│   │   │   │   ├── MemoryStore.java
│   │   │   │   └── MemoryType.java
│   │   │   │
│   │   │   └── model/
│   │   │       ├── ChatSession.java
│   │   │       ├── MessagesMapper.java
│   │   │       ├── Project.java
│   │   │       ├── ProjectCommands.java
│   │   │       ├── ProjectCommands2.java
│   │   │       └── Rules.java
│   │   │
│   │   └── resources/
│   │       └── META-INF/
│   │           └── MANIFEST.MF
│   │
│   └── test/
│       └── java/
│
└── data/                  # generated/local runtime data; ignored by Git
    ├── projects.json
    ├── chatHistory/
    ├── sendFiles/
    ├── autoSave/
    └── memory/
```
The manifest declares:

```text
Main-Class: com.lior.tracker.Chat
```

---

## Local Data and Git

Runtime data is intentionally ignored by Git:

```gitignore
data/
```

This prevents local conversations, project notes, attachments, autosaves, and long-term memories from being committed accidentally.

`AppPaths` centralizes the runtime locations:
```java
public static final Path DATA = Path.of("data/");
public static final Path PROJECTS = DATA.resolve("projects.json");
public static final Path HISTORY = DATA.resolve("chatHistory/");
public static final Path FILES = DATA.resolve("sendFiles/");
public static final Path AUTOSAVE = DATA.resolve("autoSave/");
public static final Path MEMORY = DATA.resolve("memory/");
```

Because the whole `data/` directory is ignored, the memory vault is local too. Git does not back up `data/memory/`; if the memories matter, back up or sync that folder separately.

---

## Error Handling

Current provider implementations check for missing API keys before sending a request.

HTTP failures produce an error containing the response status and, where available, the response body. Rate-limit responses are handled as API errors and the main chat loop performs a retry/fallback attempt.

For Gemini, the retry uses the Lite model. For the other providers, the same provider configuration is attempted again.

---

## Current Development Notes

The project is functional but still actively evolving.

Current areas under development include:

- Full Swing chat interface (`ChatWindow`)
- More robust command handling
- More flexible provider/model configuration
- Reduced duplicated logic between Groq and NVIDIA agents
- Improved persistence/session management
- Automated tests
- More advanced long-term memory retrieval and management, including semantic search/compression
- Memory-management commands for browsing, editing, or deleting memories from the CLI

The original long-term memory goal is now partially implemented: memories are persisted to Markdown, recalled automatically, extracted from new exchanges, and can be viewed in Obsidian. Memory compression is still unimplemented and retrieval is currently lexical/keyword-based.

`src/test/java/` currently contains no automated tests.

---

## Adding Another AI Provider

To integrate another provider:

1. Create a class implementing `AiAgent`.
2. Implement `ask(...)`, `getName()`, and `isStream()`.
3. Add the provider-specific message/request mapping.
4. Read its API key from an environment variable.
5. Return responses as `Result`.
6. Register the provider in the model-selection logic in `Chat`.

Example skeleton:
```java
public class ExampleAgent implements AiAgent {
    @Override
    public Result ask(List<ChatMessage> messages, String ver)
            throws IOException, InterruptedException {
        // Build request
        // Send request
        // Parse response
        return new Result("response", 0);
    }

    @Override
    public String getName() {
        return "Example";
    }

    @Override
    public boolean isStream() {
        return false;
    }
}
```

---

## Security

Do not commit:

- API keys
- `.env` files containing secrets
- personal conversation history
- private project notes
- files placed in `data/sendFiles/`
- long-term memories under `data/memory/` unless you intentionally want them public

The repository's `.gitignore` excludes local runtime data and common IDE/build artifacts.

If an API key is ever committed publicly, revoke it immediately and generate a new one.

---

## License

This project is licensed under the terms in [LICENSE](LICENSE).
