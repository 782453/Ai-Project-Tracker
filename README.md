# AI Project Tracker

![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.x-C71A36?logo=apachemaven&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green.svg)
![Interface](https://img.shields.io/badge/Interface-CLI-blue)

A Java command-line application that combines **multi-provider AI chat** with **lightweight project tracking**, local chat-history persistence, conversation summarization, and file-assisted prompts.

The project is designed as a personal AI workspace: choose an AI backend, keep a multi-turn conversation, attach local files, save or restore chat context, and inject project notes directly into the conversation when continuing work on a project.

---

## Table of Contents

- [Overview](#overview)
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
- [Conversation Summarization](#conversation-summarization)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Main Classes](#main-classes)
- [Data Storage](#data-storage)
- [Error Handling and Fallbacks](#error-handling-and-fallbacks)
- [Security and Privacy](#security-and-privacy)
- [Current Limitations](#current-limitations)
- [Development Roadmap](#development-roadmap)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

AI Project Tracker is a terminal-based Java application built around a small provider abstraction:

```text
User
  |
  v
Chat
  |
  +---- ProjectCommands
  |        |
  |        +---- projects.json
  |        +---- chatHistory/
  |        +---- sendFiles/
  |
  v
AiAgent
  |
  +---- GeminiAgent ------> Gemini API
  |
  +---- GroqAgent --------> Groq OpenAI-compatible API
                |
                +---- GPT-OSS
                +---- Qwen
```

Messages are kept in a `ChatSession`, transformed into the JSON structure expected by the selected provider, sent over Java's built-in `HttpClient`, and parsed with Jackson.

The application does **not** store API keys in source code. Keys are read from environment variables when an API request is made.

---

## Features

- **Multiple AI backends**
  - Google Gemini
  - OpenAI GPT-OSS through Groq
  - Qwen through Groq

- **Multi-turn chat context**
  - Previous user and model messages are sent with future requests.

- **Project tracking**
  - Create projects.
  - View projects.
  - Change project status.
  - Append notes.
  - Delete projects.
  - Inject project context into the active AI conversation.

- **Local chat-history persistence**
  - Save conversations as JSON.
  - Load previous conversations back into the active context.

- **Conversation summarization**
  - Compress a long conversation into a smaller context for later turns.

- **Local file attachments**
  - Read a file from the local `sendFiles/` directory.
  - Detect its MIME type.
  - Encode it with Base64.
  - Send it as Gemini inline data.

- **Large-text paste mode**
  - Paste multi-line text and terminate it with `eof`.

- **Usage information**
  - Displays API-reported total token usage.
  - Displays approximate request duration.

- **Token-compression prompt mode**
  - At startup, the application can inject a response-style prompt intended to reduce unnecessary token usage.

- **Provider-independent interface**
  - New AI providers can be added by implementing `AiAgent`.

---

## AI Providers and Models

The current source configuration is:

| CLI option | Displayed name | Provider | Model configured in source | Required key |
|---|---|---|---|---|
| `0` | Gemini | Google Gemini API | `gemini-3.5-flash` | `GEMINI_API_KEY` |
| fallback | Gemini | Google Gemini API | `gemini-3.5-flash-lite` | `GEMINI_API_KEY` |
| `1` | ChatGPT | Groq | `openai/gpt-oss-120b` | `GROQ_API_KEY` |
| `2` | Qwen | Groq | `qwen/qwen3.8-27b` | `GROQ_API_KEY` |

> [!NOTE]
> Option `1` is labeled **ChatGPT** in the current CLI, but the code does not use the ChatGPT product or OpenAI API. It uses the open-weight `openai/gpt-oss-120b` model through Groq.

> [!IMPORTANT]
> Model availability changes over time. If Groq returns a model-not-found error for the configured Qwen model, check Groq's current supported-model list and update `groqModel` in `Chat.java`.

### Provider endpoints

The current implementation communicates directly with:

- Gemini GenerateContent API
- Groq's OpenAI-compatible `/chat/completions` endpoint

No provider SDK is required.

---

## Requirements

- **JDK 25**
- **Apache Maven**
- Internet connection
- A Gemini API key for Gemini requests
- A Groq API key for Groq requests

The Maven compiler configuration currently targets Java 25:

```xml
<maven.compiler.source>25</maven.compiler.source>
<maven.compiler.target>25</maven.compiler.target>
```

The project uses Java APIs such as `List.getLast()`, so running it on an older JDK is not recommended.

---

## Installation

Clone the repository:

```bash
git clone https://github.com/<your-username>/ai-project-tracker.git
cd ai-project-tracker
```

Build the project:

```bash
mvn clean package
```

The main application class is:

```text
com.lior.tracker.Chat
```

### IntelliJ IDEA

The simplest development workflow is:

1. Open the project directory in IntelliJ IDEA.
2. Let IntelliJ import the Maven project.
3. Make sure the Project SDK is set to **JDK 25**.
4. Open:

```text
src/main/java/com/lior/tracker/Chat.java
```

5. Run `Chat.main()`.

> [!NOTE]
> The current `pom.xml` does not configure a shaded/fat executable JAR. `mvn package` is useful for compiling and packaging the project, but running from the IDE is currently the most straightforward option.

---

## API Keys

The application expects API keys in environment variables.

### Windows PowerShell — current terminal session

```powershell
$env:GEMINI_API_KEY="your-gemini-key"
$env:GROQ_API_KEY="your-groq-key"
```

### Windows — persistent environment variables

```powershell
setx GEMINI_API_KEY "your-gemini-key"
setx GROQ_API_KEY "your-groq-key"
```

Open a new terminal after using `setx`.

### macOS / Linux

```bash
export GEMINI_API_KEY="your-gemini-key"
export GROQ_API_KEY="your-groq-key"
```

You only need the key for the provider you intend to use, with one exception: the current experimental Groq file-attachment path can hand a file to Gemini first and therefore also requires `GEMINI_API_KEY`.

### Never commit API keys

Do not place real keys inside:

- Java source files
- `pom.xml`
- `projects.json`
- committed IDE configuration
- a committed `.env` file

If a key has ever been committed or publicly shared, revoke/rotate it at the provider.

---

## Running the Application

On startup, the CLI asks you to select an AI model:

```text
Ai models list:
[0] Gemini
[1] ChatGPT
[2] Qwen
Choose Ai model:
```

Then it asks:

```text
Maximize token usage (y/n)?
```

Selecting `y` currently injects `MAX_PROMPT`, which is actually designed to make responses **more compressed and token-efficient** by reducing filler and repetition.

After initialization:

```text
Write a message:
```

You can enter a normal message or one of the slash commands below.

---

## Commands

Enter `/?` inside the application to display the built-in command list.

| Command | Purpose |
|---|---|
| `/?` | Show available commands |
| `/nchat` | Start a fresh conversation |
| `/nproject` | Create a new tracked project |
| `/projects` | List projects and open project actions |
| `/sprojects` | Save the current project list |
| `/send` | Attach a file from `sendFiles/` |
| `/paste` | Paste a large multi-line text block |
| `/summarize` | Replace a sufficiently large conversation with a compact AI-generated summary |
| `/save` | Save the accumulated chat history to disk |
| `/load` | Load a previous chat-history JSON file |
| `/exit` | Exit the application |

Some commands continue by asking for another normal message before returning to the model.

---

## Project Tracking

Projects are represented by the `Project` class.

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

The application asks for:

```text
Project name:
Project status (ACTIVE, BLOCKED, PAUSED, DONE):
Last updated (YYYY-MM-DD):
Project notes:
```

The updated project collection is then written to `projects.json`.

### Managing projects

Use:

```text
/projects
```

Select a project by its numeric index.

Available project actions are:

| Input | Action |
|---|---|
| `w` | Work on the project |
| `c` | Change status |
| `a` | Append notes |
| `DEL` | Delete the project |

Choosing `w` injects the selected project's name and notes into the current AI context:

```text
We'll continue working on this project today: <project name>
Notes: <project notes>
```

This allows the AI conversation to immediately continue with project-specific context.

### Example `projects.json`

```json
[
  {
    "name": "Example Project",
    "status": "ACTIVE",
    "lastUpdated": "YYYY-MM-DD",
    "notes": "Initial project notes"
  }
]
```

---

## Chat History

The application can save conversations as local JSON files.

Use:

```text
/save
```

You will be asked for a chat name.

Saved filenames contain:

- the chosen chat name
- the current date
- a generated UUID

Example shape:

```text
chatHistory/
└── backend-refactor-YYYY-MM-DD-<uuid>.json
```

### Loading previous context

Use:

```text
/load
```

The program lists saved history files and asks you to select one by index.

The selected message history is loaded into the active `ChatSession`, allowing the next message to continue from previous context.

### Attachment handling in saved history

The application does not intentionally store the complete Base64 attachment payload in its normal accumulated history. For an attached user message, history records a text reference similar to:

```text
A file was attached here: <filename>
```

This helps prevent saved history files from becoming unnecessarily large.

---

## File Attachments

Files used by `/send` are expected inside:

```text
sendFiles/
```

Example:

```text
sendFiles/
├── specification.pdf
├── data.json
└── notes.txt
```

Run:

```text
/send
```

Then enter the filename exactly as it appears in that directory.

The application:

1. Verifies that the file exists.
2. Reads the file bytes.
3. Encodes the bytes with Base64.
4. Detects the MIME type with `Files.probeContentType(...)`.
5. Creates a `ChatMessage` containing:
   - prompt text
   - Base64 file data
   - MIME type
   - filename

For Gemini, `MessagesMapper` creates separate text and inline-data parts:

```json
{
  "role": "user",
  "parts": [
    {
      "text": "Analyze this file"
    },
    {
      "inline_data": {
        "mime_type": "application/pdf",
        "data": "<base64>"
      }
    }
  ]
}
```

### Groq attachments

The current Groq attachment flow is experimental.

When the most recent message contains inline file data, `GroqAgent` currently hands the file-containing message to `GeminiAgent` first. The current implementation then terminates the process with `System.exit(0)`, so a complete Gemini-to-Groq handoff is not yet finished.

For reliable file attachments in the current version, use the Gemini option.

---

## Conversation Summarization

Use:

```text
/summarize
```

The command is enabled when the session's recorded token count is at least `1000`.

The application asks the selected AI model to create a compact context summary that preserves items such as:

- important facts
- user preferences
- decisions
- technical details
- class, method, variable, API, and model names
- constraints
- unresolved tasks
- important code details
- recent conversation state

After the summary is returned, the active message list is reduced so that future requests can continue from a smaller context.

This is useful when long conversations become expensive or repetitive to resend.

---

## Architecture

### Provider abstraction

All AI providers implement:

```java
public interface AiAgent {
    Result ask(List<ChatMessage> messages, String ver)
            throws IOException, InterruptedException;

    String getName();
}
```

This keeps the chat loop independent of a specific API.

A provider is responsible for:

1. Reading its environment-variable API key.
2. Mapping the shared message format into provider JSON.
3. Sending the HTTP request.
4. Parsing the response.
5. Returning a shared `Result`.

### Shared result type

Provider responses are normalized into:

```java
public record Result(String text, int tokens) {}
```

This gives the main chat loop a provider-independent response format.

### Message mapping

`MessagesMapper` contains separate builders for:

- Gemini `contents`
- Groq `messages`

For Groq, an internal role named:

```text
model
```

is converted to:

```text
assistant
```

to match the OpenAI-compatible message format.

---

## Project Structure

```text
ai-project-tracker/
├── pom.xml
├── .gitignore
├── LICENSE
├── README.md
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/lior/tracker/
│   │   │       ├── AppPaths.java
│   │   │       ├── Chat.java
│   │   │       ├── ChatMessage.java
│   │   │       │
│   │   │       ├── agent/
│   │   │       │   ├── AiAgent.java
│   │   │       │   ├── GeminiAgent.java
│   │   │       │   ├── GroqAgent.java
│   │   │       │   └── Result.java
│   │   │       │
│   │   │       └── model/
│   │   │           ├── ChatSession.java
│   │   │           ├── MessagesMapper.java
│   │   │           ├── Project.java
│   │   │           ├── ProjectCommands.java
│   │   │           └── temp.java
│   │   │
│   │   └── resources/
│   │       └── META-INF/
│   │           └── MANIFEST.MF
│   │
│   └── test/
│       └── java/
│
├── projects.json       # local runtime data
├── chatHistory/        # local runtime data
└── sendFiles/          # local files prepared for attachment
```

Build output such as `target/` and IDE-generated artifacts are intentionally omitted from this structure.

---

## Main Classes

### `Chat`

Application entry point and main conversation loop.

Responsibilities include:

- model selection
- optional compressed-response prompt injection
- user input
- command dispatch
- request timing
- provider calls
- retry/fallback handling
- token display
- global history accumulation

---

### `ChatSession`

Holds mutable state for the active conversation:

```text
projects
messages
userMessage
tokens
```

It also provides access to the two most recent messages for history tracking.

---

### `ChatMessage`

Shared message object containing:

```text
role
text
inline_data
mimeType
filename
```

The file-related fields are optional and are used for attachment requests.

---

### `AiAgent`

Provider interface.

Any future provider can integrate with the application by implementing this interface.

---

### `GeminiAgent`

Handles:

- `GEMINI_API_KEY`
- Gemini request construction
- Java `HttpClient`
- Gemini response parsing
- total-token extraction
- default and lightweight Gemini model selection

---

### `GroqAgent`

Handles:

- `GROQ_API_KEY`
- Groq's OpenAI-compatible chat-completions endpoint
- configured GPT-OSS/Qwen model selection
- response parsing
- total-token extraction
- experimental file-to-Gemini forwarding

---

### `MessagesMapper`

Uses Jackson's tree model to construct provider-specific request JSON.

Responsibilities include:

- Gemini message parts
- Gemini inline attachment data
- Groq role conversion
- Groq model field
- provider message arrays

---

### `Project`

Represents one tracked project and provides:

- status enum
- getters/setters
- note appending
- JSON loading through Jackson

---

### `ProjectCommands`

Implements the slash-command system and local persistence.

It handles:

- new chats
- project creation and management
- file attachment preparation
- summarization
- chat save/load
- large-text paste mode
- exit flow

---

### `AppPaths`

Defines intended centralized paths:

```java
DATA
PROJECTS
HISTORY
FILES
```

The current command implementation still uses direct root-level paths such as `projects.json`, `chatHistory/`, and `sendFiles/`, so this class is not yet consistently wired into persistence.

---

### `temp`

Contains an older/experimental version of chat and command logic.

It is not the main application entry point.

---

## Data Storage

The application currently stores local data as unencrypted files.

### Projects

```text
projects.json
```

### Saved chats

```text
chatHistory/*.json
```

### Files prepared for AI attachment

```text
sendFiles/*
```

### Important

These files may contain:

- private project notes
- conversation content
- source files
- documents submitted to an AI provider

Treat them as local runtime data unless you intentionally want to publish them.

---

## Error Handling and Fallbacks

### Missing API key

The provider throws an error if its environment variable is missing or blank.

Example:

```text
GEMINI_API_KEY environment variable is required.
```

### API rate limits

Both provider implementations detect HTTP `429`.

The main chat loop then retries using:

```java
agent.ask(messages, "lite");
```

For Gemini:

```text
default -> gemini-3.5-flash
lite    -> gemini-3.5-flash-lite
```

For `GroqAgent`, the `ver` argument is not currently used, so the retry uses the same configured Groq model.

### Other API errors

Non-`200` responses are surfaced as runtime errors containing:

- HTTP status code
- provider response body

---

## Security and Privacy

### API keys

Keys are loaded with:

```java
System.getenv("GEMINI_API_KEY")
System.getenv("GROQ_API_KEY")
```

This is safer than committing credentials into the repository.

### Local data

`projects.json` and files under `chatHistory/` are stored as plain JSON and are **not encrypted**.

### External AI providers

Messages sent through the application are transmitted to the selected provider.

Attached files can also be transmitted to an external AI service.

Do not send confidential or sensitive material unless you are comfortable with the selected provider's data-handling terms.

### Repository hygiene

For a public repository, local runtime data should normally stay out of Git:

```gitignore
projects.json
chatHistory/
sendFiles/
data/
out/
target/
```

Also keep IDE-local configuration such as `.idea/workspace.xml` out of version control.

---

## Current Limitations

This is an actively evolving project. The current source has several areas that can be improved:

1. **Groq file handoff is incomplete**  
   The attachment branch currently calls Gemini and then exits the JVM with `System.exit(0)`.

2. **Qwen model availability is provider-dependent**  
   `Chat.java` currently contains `qwen/qwen3.8-27b`. If Groq does not expose that identifier, it must be changed to an active Groq model ID.

3. **The CLI label `ChatGPT` is imprecise**  
   The selected model is `openai/gpt-oss-120b` served by Groq, not the ChatGPT product/API.

4. **Groq fallback mode is not implemented**  
   `GroqAgent.ask(..., ver)` currently ignores `ver`.

5. **Path handling is inconsistent**  
   `AppPaths` defines a `data/` layout, while `ProjectCommands` currently uses root-level runtime paths.

6. **Chat-history loading contains a Windows-style path**  
   `Path.of("chatHistory\\")` is platform-specific and should eventually be replaced with a platform-neutral path.

7. **No automated tests are currently present**  
   `src/test/java` exists but contains no test suite.

8. **No self-contained release JAR is configured in Maven**  
   A shade/assembly plugin would make command-line distribution easier.

9. **Legacy code remains in `temp.java`**  
   It can eventually be removed or moved to a dedicated experimental area.

10. **Project status/date input is minimally validated**  
    Invalid status strings can leave the status unset, and the date is currently accepted as raw text.

---

## Development Roadmap

Possible next improvements:

- [ ] Replace the startup model switch with a provider/model configuration object.
- [ ] Rename the GPT-OSS CLI option so it accurately reflects the backend.
- [ ] Complete Groq file-attachment support.
- [ ] Move all runtime data under the centralized `AppPaths.DATA` directory.
- [ ] Replace Windows-specific paths with platform-neutral `Path` composition.
- [ ] Add automatic `LocalDate` updates for project modifications.
- [ ] Validate project status and date input.
- [ ] Add unit tests for `MessagesMapper`.
- [ ] Add tests for project serialization/deserialization.
- [ ] Add integration tests with mocked HTTP responses.
- [ ] Configure Maven to build an executable fat JAR.
- [ ] Remove or archive legacy `temp.java`.
- [ ] Add structured logging instead of direct `System.out` / `System.err` calls.
- [ ] Improve provider-specific retry and rate-limit handling.
- [ ] Add streaming responses.
- [ ] Add a graphical interface or web client on top of the existing `AiAgent` abstraction.

---

## Contributing

Contributions, refactors, bug reports, and feature suggestions are welcome.

A typical contribution flow:

```bash
git checkout -b feature/my-feature
git add .
git commit -m "Add my feature"
git push origin feature/my-feature
```

Then open a pull request.

Before committing:

```bash
git status
git diff --cached
```

Make sure no API keys, private chat histories, or local attachment files are staged.
---

## License

This project is licensed under the **MIT License**.

You are free to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the software under the terms of the license.

See [`LICENSE`](LICENSE) for the full license text.
