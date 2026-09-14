# AI Project Tracker

![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.x-C71A36?logo=apachemaven&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green.svg)
![Interface](https://img.shields.io/badge/Interface-CLI-blue)

A Java command-line application that combines **multi-provider AI chat** with **lightweight project tracking**, local chat-history persistence, conversation summarization, and file-assisted prompts.

The application can chat with Gemini, GPT-OSS and Qwen through Groq, or NVIDIA Nemotron; save and restore conversations; attach local files; maintain project notes; and inject project context into the active AI conversation.

> **Development status:** active / experimental. The core application works, but several areas listed under [Current limitations](#current-limitations) are still being improved.

---

## Table of Contents

- [Features](#features)
- [AI Providers and Models](#ai-providers-and-models)
- [Requirements](#requirements)
- [Installation](#installation)
- [API Keys](#api-keys)
- [Running the Application](#running-the-application)
- [Commands](#commands)
- [Project Tracking](#project-tracking)
- [Chat History](#chat-history)
- [File Attachments](#file-attachments)
- [Conversation Rules and Summarization](#conversation-rules-and-summarization)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Data Storage](#data-storage)
- [Error Handling](#error-handling)
- [Security and Privacy](#security-and-privacy)
- [Current Limitations](#current-limitations)
- [Development Roadmap](#development-roadmap)
- [License](#license)

---

## Features

- Multi-provider AI chat using a common `AiAgent` interface.
- Google Gemini support.
- GPT-OSS and Qwen support through Groq.
- NVIDIA Nemotron support through NVIDIA NIM.
- Multi-turn conversation context.
- Project creation and status tracking.
- Project notes that can be injected into the active chat.
- Local JSON chat-history save/load.
- Manual and automatic conversation summarization.
- Optional token-compression instructions.
- Local file attachment support.
- Multi-line paste mode.
- Swing history viewer.
- API-reported token usage and request-duration display.

---

## AI Providers and Models

The current source code exposes four startup options:

| CLI option | Display name | Provider | Model ID | Required environment variable |
|---:|---|---|---|---|
| `0` | Gemini | Google Gemini API | `gemini-3.5-flash` | `GEMINI_API_KEY` |
| fallback | Gemini Lite | Google Gemini API | `gemini-3.5-flash-lite` | `GEMINI_API_KEY` |
| `1` | ChatGPT | Groq | `openai/gpt-oss-120b` | `GROQ_API_KEY` |
| `2` | Qwen | Groq | `qwen/qwen3.8-27b` | `GROQ_API_KEY` |
| `3` | Nemotron | NVIDIA NIM | `nvidia/nemotron-3-ultra-550b-a55b` | `NVIDIA_API_KEY` |

> **Note:** option `1` is labeled `ChatGPT` in the CLI, but the application does **not** call the ChatGPT product or OpenAI API. It sends requests to Groq using the `openai/gpt-oss-120b` model ID.

### Provider endpoints used by the code

- Gemini: `https://generativelanguage.googleapis.com/v1/models/{model}:generateContent`
- Groq: `https://api.groq.com/openai/v1/chat/completions`
- NVIDIA: `https://integrate.api.nvidia.com/v1/chat/completions`

No provider SDK is required. Requests are sent with Java's built-in `HttpClient` and JSON is handled with Jackson.

---

## Requirements

- **JDK 25**
- **Apache Maven 3.x**
- Internet connection
- At least one API key for the provider you intend to use

The Maven configuration currently targets Java 25:

```xml
<maven.compiler.source>25</maven.compiler.source>
<maven.compiler.target>25</maven.compiler.target>
```

The code also uses modern collection methods such as `List.getLast()` and `removeLast()`.

---

## Installation

Clone the repository:

```bash
git clone https://github.com/<your-username>/ai-project-tracker.git
cd ai-project-tracker
```

Build it:

```bash
mvn clean package
```

Or open the project directly in IntelliJ IDEA as a Maven project.

### IntelliJ IDEA

1. Open the repository directory.
2. Allow IntelliJ to import the Maven project.
3. Make sure the project SDK is set to **JDK 25**.
4. Add the API keys you need under:

```text
Run -> Edit Configurations -> Environment variables
```

5. Run `com.lior.tracker.Chat`.

---

## API Keys

The application reads API keys from environment variables at request time.

### Required names

```text
GEMINI_API_KEY
GROQ_API_KEY
NVIDIA_API_KEY
```

You only need the key for the provider you use, with one exception: Groq and NVIDIA file attachments are currently preprocessed through Gemini, so attachment use with those providers also requires `GEMINI_API_KEY`.

### Windows PowerShell - current terminal

```powershell
$env:GEMINI_API_KEY="your_key_here"
$env:GROQ_API_KEY="your_key_here"
$env:NVIDIA_API_KEY="your_key_here"
```

### Windows - persistent variables

```powershell
setx GEMINI_API_KEY "your_key_here"
setx GROQ_API_KEY "your_key_here"
setx NVIDIA_API_KEY "your_key_here"
```

Open a new terminal after using `setx`.

### macOS / Linux

```bash
export GEMINI_API_KEY="your_key_here"
export GROQ_API_KEY="your_key_here"
export NVIDIA_API_KEY="your_key_here"
```

Do **not** hard-code API keys in Java source files or commit them to Git.

---

## Running the Application

Run the main class from IntelliJ, or use your preferred Maven/Java execution workflow.

At startup the CLI displays:

```text
Ai models list:
[0] Gemini
[1] ChatGPT
[2] Qwen
[3] Nemotron
Choose Ai model:
```

If Nemotron is selected, the program additionally offers an optional system instruction:

```text
Would you like to add instructions (y/n)?
```

After that, the chat loop starts.

---

## Commands

Type `/?` in the chat to open page 1 of the command menu.

### Page 1

| Command | Action |
|---|---|
| `/nchat` | Start a fresh chat session |
| `/nproject` | Create a new tracked project |
| `/projects` | List/select project operations |
| `/sprojects` | Save the current project list |
| `/send` | Attach a file from `sendFiles/` |
| `/paste` | Paste multi-line text until `eof` |
| `/summarize` | Manually summarize a sufficiently large chat |
| `/save` | Save full chat history to JSON |
| `/load` | Load a previously saved chat |
| `/++` | Open command page 2 |

### Page 2

| Command | Action |
|---|---|
| `/history` | Open the active model context in a Swing window |
| `/fhistory` | Open the full accumulated session history |
| `/rules` | View/change token-related chat rules |
| `/resend` | Experimental resend/regeneration command |
| `/exit` | Exit the application |
| `/..` | Return to page 1 |

---

## Project Tracking

Projects are represented by `Project` and contain:

```text
name
status
lastUpdated
notes
```

Supported statuses are:

```text
ACTIVE
BLOCKED
PAUSED
DONE
```

### Create a project

Use:

```text
/nproject
```

The CLI asks for:

1. Project name
2. Project status
3. Last-updated date
4. Notes

The project list is then saved to `projects.json`.

### Manage projects

Use:

```text
/projects
```

After selecting a project, the current options are:

```text
[w]   Work on project
[c]   Change project status
[a]   Add notes
[DEL] Delete project
```

`w` injects the selected project's name and notes into the active conversation so the AI can continue working with that context.

---

## Chat History

The application maintains two related forms of chat state:

- `ChatSession.messages`: active model context.
- `Chat.msgHistory`: accumulated history used for full-history viewing and saving.

### Save history

Use:

```text
/save
```

The program asks for a chat name and writes a JSON file under:

```text
chatHistory/
```

Saved filenames include the supplied name, current date, and a UUID.

### Load history

Use:

```text
/load
```

The CLI lists regular files in `chatHistory/`, lets you choose one by index, deserializes it into `ChatMessage` objects, and copies the loaded messages into the active `ChatSession`.

### Saved attachments

When a message originally contained inline Base64 file data, the long Base64 payload is not copied into full history. Instead, the saved history records a text note indicating that a file was attached and preserves its filename.

---

## File Attachments

Use:

```text
/send
```

Files are read from:

```text
sendFiles/
```

The current flow is:

1. Enter a filename.
2. The program checks whether it exists.
3. The complete file is read into memory.
4. It is Base64 encoded.
5. `Files.probeContentType()` is used to determine the MIME type.
6. A `ChatMessage` stores the prompt, Base64 data, MIME type, and filename.

### Gemini attachments

Gemini receives the Base64 data directly as an `inline_data` part.

### Groq and NVIDIA attachments

Groq and NVIDIA currently do not receive the raw attachment directly from this application. The attachment message is first sent to `GeminiAgent`; Gemini's returned text is then inserted into the conversation before the Groq or NVIDIA request is made.

Because of this design, sending files while using Groq or NVIDIA currently requires a valid `GEMINI_API_KEY` as well.

---

## Conversation Rules and Summarization

`Rules` contains two optional behaviors:

```text
maxTokens
autoSummarize
```

Both are `false` by default when the application starts.

Use:

```text
/rules
```

to view and optionally replace the current rule configuration.

The current rule editor expects Java boolean input:

```text
true
false
```

### `maxTokens`

When enabled, a compression-oriented instruction is inserted into model context. Nemotron receives it as a `system` message. Other providers receive it as a user/model pair.

### Manual summarization

Use:

```text
/summarize
```

The command is allowed when the current API-reported token count is at least `1000`.

### Automatic summarization

When `autoSummarize` is enabled, the chat loop attempts automatic summarization once the stored token count reaches `10000`.

The summarization prompt attempts to preserve important facts, preferences, technical details, decisions, code identifiers, constraints, unresolved work, and recent conversation context while removing filler and repetition.

For Gemini/Groq sessions, summarization is currently performed through Gemini. For Nemotron sessions, it is performed through Nemotron.

---

## Architecture

The application uses a small provider abstraction:

```text
                       +-------------------+
                       |       Chat        |
                       +---------+---------+
                                 |
                  +--------------+--------------+
                  |                             |
                  v                             v
        +------------------+          +-------------------+
        |  ChatSession     |          | ProjectCommands   |
        +------------------+          +-------------------+
                  |
                  v
             +---------+
             | AiAgent |
             +----+----+
                  |
      +-----------+-----------+
      |           |           |
      v           v           v
 GeminiAgent   GroqAgent   NvidiaAgent
      |           |           |
      v           v           v
  Gemini API   Groq API   NVIDIA NIM
```

### `AiAgent`

Provider implementations expose the same core contract:

```java
Result ask(List<ChatMessage> messages, String ver);
String getName();
```

### `Result`

`Result` is a Java record containing:

```text
text
tokens
```

### `MessagesMapper`

`MessagesMapper` converts internal `ChatMessage` objects into provider-specific JSON:

- Gemini `contents`/`parts`
- Groq OpenAI-compatible `messages`
- NVIDIA OpenAI-compatible `messages` plus Nemotron generation parameters

### `ChatSession`

`ChatSession` stores:

- tracked projects
- active messages
- current user message
- latest reported token count

### `HistoryWindow`

`HistoryWindow` provides a simple read-only Swing representation of either active context or full accumulated history.

---

## Project Structure

```text
ai-project-tracker/
├── LICENSE
├── README.md
├── pom.xml
├── projects.json
├── chatHistory/
├── sendFiles/
└── src/
    ├── main/
    │   ├── java/com/lior/tracker/
    │   │   ├── AppPaths.java
    │   │   ├── Chat.java
    │   │   ├── ChatMessage.java
    │   │   ├── agent/
    │   │   │   ├── AiAgent.java
    │   │   │   ├── GeminiAgent.java
    │   │   │   ├── GroqAgent.java
    │   │   │   ├── NvidiaAgent.java
    │   │   │   └── Result.java
    │   │   ├── gui/
    │   │   │   └── HistoryWindow.java
    │   │   └── model/
    │   │       ├── ChatSession.java
    │   │       ├── MessagesMapper.java
    │   │       ├── Project.java
    │   │       ├── ProjectCommands.java
    │   │       ├── ProjectCommands2.java
    │   │       └── Rules.java
    │   └── resources/
    │       └── META-INF/MANIFEST.MF
    └── test/
        └── java/
```

`src/test/java/` currently exists but contains no automated tests.

---

## Data Storage

The current working implementation uses these root-level paths:

```text
projects.json
chatHistory/
sendFiles/
```

`AppPaths.java` already defines a planned `data/` layout:

```text
data/projects.json
data/chatHistory/
data/sendFiles/
```

but those constants are **not yet wired into the rest of the application**. Until that refactor is completed, moving the runtime files into `data/` will break the existing persistence code.

---

## Error Handling

### Missing API keys

Each provider checks its required environment variable and throws an `IllegalArgumentException` if it is missing or blank.

### HTTP errors

For all current providers:

- HTTP `429` is reported as a quota/rate-limit error.
- Any non-`200` response throws a runtime error that includes the HTTP status and response body.

### Fallback behavior

The main chat loop currently retries once after a `RuntimeException`.

For Gemini, the retry switches from:

```text
gemini-3.5-flash
```

to:

```text
gemini-3.5-flash-lite
```

For Groq and NVIDIA, the `ver` argument does not currently select a different model, so their retry repeats essentially the same request.

---

## Security and Privacy

### API keys

API keys are read from environment variables and are not stored directly in the Java source.

Gemini currently places its key in the request URL query string. A future improvement is to move it to the `x-goog-api-key` request header.

### Local files

The application may store local project notes and conversation history in plaintext JSON. Treat these files as potentially sensitive.

### External providers

Prompts, conversation history, and attachments are sent to the selected external AI provider. Groq/NVIDIA attachment preprocessing additionally sends the attachment to Gemini.

### Repository hygiene

This source snapshot does **not** contain a `.gitignore` file. Before publishing the repository, create one and at minimum consider excluding:

```gitignore
.idea/
*.iml
target/
projects.json
chatHistory/
sendFiles/
data/
.env
*.env
```

Whether `projects.json` should be ignored depends on whether it is intended as example data or private working data. If you want an example in Git, prefer a separate file such as `projects.example.json`.

---

## Current Limitations

The following limitations reflect the current source code rather than planned behavior:

1. `AppPaths` is defined but not yet used by persistence and attachment code.
2. Automatic summarization does not currently recalculate/reset `ChatSession.tokens` after replacing the conversation with its summary.
3. If automatic summarization fails after the summary prompt is appended, that prompt can remain in active context.
4. `/load` clears global history before a successful selection is guaranteed, so cancelling/invalid loading can replace active context with an empty history.
5. `/resend` is still experimental and does not currently regenerate the normal completed assistant reply flow.
6. API retry sleep values are currently `20` milliseconds, which is too short to be useful for most rate-limit recovery.
7. Retry logic does not distinguish permanent errors such as `400`/`401` from transient network, `429`, or `5xx` errors.
8. `HttpClient` instances are recreated for requests and no explicit request timeout is configured.
9. Provider response parsing assumes normal success fields such as `usage`, `usageMetadata`, and the first candidate/choice are present.
10. Groq/NVIDIA attachment preprocessing mutates the active conversation by inserting Gemini output and a synthetic user message.
11. `/send` reads the complete attachment into memory and currently has no file-size limit.
12. `/send` does not yet normalize and verify the requested path stays inside `sendFiles/`.
13. Chat-history names are not sanitized before being used as filenames.
14. Empty project lists are saved by inserting a placeholder project instead of serializing `[]`.
15. Project status and date values are accepted as unchecked strings at the CLI boundary; invalid status text can leave `status` unset.
16. `Rules()` expects `true`/`false`, not `y`/`n`.
17. The multi-line paste delimiter is based on `eof\n` and may behave differently across platforms/newline formats.
18. Chat/session/provider state is heavily static/global, which makes testing and future multiple-session support harder.
19. No automated tests currently exist under `src/test/java`.
20. No `.gitignore` is present in this source snapshot.

---

## Development Roadmap

Useful next steps, roughly in priority order:

1. Finish the `AppPaths` migration and create runtime directories centrally.
2. Fix load cancellation/failure so existing history is not destroyed.
3. Complete `/resend` as a real response-regeneration flow.
4. Make summarization operate on a copied message list and reset/recalculate tokens after success.
5. Introduce shared HTTP infrastructure with timeouts and structured error classification.
6. Implement sensible retry/backoff behavior for `429`, transient network failures, and selected `5xx` responses.
7. Move Gemini authentication from the query string to the API-key request header.
8. Move file preprocessing out of provider agents so agents do not mutate conversation state.
9. Add path normalization, attachment-size validation, and filename sanitization.
10. Save an empty project list as `[]` rather than creating a placeholder project.
11. Validate project status/date input.
12. Replace recursive command navigation with a simpler loop/state-machine design.
13. Reduce static/global dependencies by passing model/session configuration through constructors.
14. Add unit tests for commands, history loading, summarization, mapping, persistence, and path handling.
15. Add a `.gitignore` before publishing private runtime state to GitHub.

---

## License

This project is licensed under the **MIT License**. See [`LICENSE`](LICENSE).

Copyright (c) 2026 Lior Lazary.
