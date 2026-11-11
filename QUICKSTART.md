# LogParser Pro — Quick Start

## Prerequisites

- JDK 21 (the Gradle toolchain targets Java 21; IntelliJ Platform 2025.1+)
- Git

## Build and run

```bash
git clone https://github.com/JirakJ/logparser-premium-plugin.git
cd logparser-premium-plugin
./gradlew buildPlugin        # plugin ZIP in build/distributions/
./gradlew runIde             # sandbox IDE with the plugin installed
```

## Try it

1. Create `test.log`:

   ```log
   2024-01-15 10:30:45.123 INFO [main] Starting application
   2024-01-15 10:30:46.456 DEBUG [worker-1] Processing request X-Request-ID: abc-123-def
   2024-01-15 10:30:47.789 ERROR [worker-1] Failed to connect to database
   java.sql.SQLException: Connection refused
       at com.example.Database.connect(Database.java:45)
       at com.example.App.main(App.java:12)
   2024-01-15 10:30:48.012 WARN [main] Retrying connection...
   ```

2. Open it in the sandbox IDE — the text editor highlights levels; the **LogParser** tab shows the table view.
3. **Tools → LogParser Pro** offers Analyze Log, Group Exceptions, Show Error Timeline, Export Log…,
   Redact Secrets to File… and Diff With Another Log….
4. Select `abc-123-def` → right-click → **LogParser Pro → Find Correlation ID**.
5. The **LogParser Pro** tool window (bottom) has Log Viewer, Search, Correlation and Statistics tabs.
   In **Search**, tick **Grok** and enter `%{LOGLEVEL:level} \[%{NOTSPACE:thread}\] %{GREEDYDATA:msg}` to see each hit's fields.

## Checks

```bash
./gradlew detekt test        # static analysis + unit tests
./gradlew verifyPlugin       # JetBrains Plugin Verifier (also run in CI)
```

CI runs the same gates on GitHub-hosted runners for every pull request.
