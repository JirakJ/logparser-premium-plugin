# LogParser Pro

Advanced log file analysis plugin for JetBrains IDEs with colour-coded log levels, filtering, pattern matching, correlation tracking, and ML-powered anomaly detection.

## Free Features

- **Colour-coded log levels** — ERROR, WARN, INFO, DEBUG, TRACE highlighted for instant visibility
- **Dedicated log editor** — Custom editor tab for `.log`, `.txt`, and `.out` files with table view
- **Log level filtering** — Filter entries by severity with a single click
- **Keyword search** — Full-text search across log entries
- **Pattern library** — Auto-detection of Spring Boot, Node.js, Python, and Java formats
- **Stack trace folding** — Consecutive stack trace lines are folded into the parent entry
- **Regex pattern extraction** — Generate regex patterns from selected text (UUIDs, IPs, timestamps, numbers)
- **Configurable settings** — Max lines, default filter level, line numbers, auto-refresh

## Premium Features (30-day free trial)

- **Correlation ID tracking** — Track request flows across log entries by correlation/trace ID
- **Advanced search** — Regex, fuzzy (Levenshtein distance), and proximity search
- **Log indexing** — Multi-threaded indexing for instant search across large files
- **ML log classification** — Categorise entries into database, network, security, performance, business logic, and system
- **Anomaly detection** — Statistical anomaly scoring based on feature-space distance
- **Pattern discovery** — Automatic grouping of similar messages with normalisation

## Installation

1. **JetBrains Marketplace**: Settings → Plugins → Marketplace → Search "LogParser Pro"
2. **Manual**: Download from [Releases](https://github.com/JirakJ/logparser-premium-plugin/releases) → Settings → Plugins → ⚙️ → Install from disk

## Requirements

- IntelliJ IDEA 2023.2+ (build 232+) or any JetBrains IDE
- JDK 17+

## Configuration

Settings → Tools → LogParser Pro

## Usage

### Open a Log File
Menu → LogParser Pro → **Open Log File** — select any `.log`, `.txt`, or `.out` file.

### Track Correlation IDs (Premium)
Menu → LogParser Pro → **Track Transaction ID** — enter a correlation ID to search the current file.

### Find Correlation IDs (Premium)
Select text in the editor → right-click → **Find Correlation ID** to extract and search for correlation identifiers.

### Extract Patterns
Select text in the editor → right-click → **Extract Pattern** to generate a regex with named capture groups.

### Manage License
Menu → LogParser Pro → **Manage License** to start a trial, activate a key, or view your license status.

## Building from Source

```bash
./gradlew buildPlugin
```

## Testing

```bash
./gradlew test
```

## License

Copyright © 2026 Jakub Jirák. All rights reserved.
