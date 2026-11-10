# LogParser Pro

[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)

Advanced log file analysis plugin for JetBrains IDEs with colour-coded log levels, filtering, pattern matching, correlation tracking, and ML-powered anomaly detection.

## Features

- **Colour-coded log levels** — ERROR, WARN, INFO, DEBUG, TRACE highlighted for instant visibility
- **Syntax highlighting** — Full language-level syntax highlighting in the text editor
- **Dedicated log editor** — Custom editor tab for `.log` and `.out` files with 6-column table view (Line, Timestamp, Level, Thread, Logger, Message)
- **Level toggle buttons** — Filter entries by severity with toggle buttons for each level
- **Keyword search** — Live filtering across log entries with search field
- **Pattern library** — Auto-detection of Spring Boot, Node.js, Python, and Java log formats
- **Stack trace folding** — Consecutive stack trace lines are folded into the parent entry
- **Regex pattern extraction** — Generate regex patterns from selected text (UUIDs, IPs, timestamps, numbers)
- **Rich configurable settings** — Max lines, font size, date format, export format, theme colours, correlation patterns, wrap lines
- **Correlation ID tracking** — Track request flows across log entries by correlation/trace ID
- **Advanced search** — Regex, fuzzy (Levenshtein distance), and simple text search
- **Log indexing** — Multi-threaded indexing for instant search across large files
- **ML log classification** — Categorise entries into database, network, security, performance, business logic, and system
- **Anomaly detection** — Statistical anomaly scoring based on feature-space distance
- **Pattern discovery** — Automatic grouping of similar messages with normalisation
- **Statistics dashboard** — Level distribution, top 10 loggers, entries per hour, error/warning rates
- **Export** — Export filtered results to CSV, JSON, or plain text
- **Context menu** — Right-click: Copy Line, Copy Message, Track Correlation ID, Go to Line
- **Tool window tabs** — Log Viewer, Search, Correlation, and Statistics tabs

## Installation

1. **JetBrains Marketplace**: Settings → Plugins → Marketplace → Search "LogParser Pro"
2. **Manual**: Download from [Releases](https://github.com/JirakJ/logparser-premium-plugin/releases) → Settings → Plugins → ⚙️ → Install from disk

## Requirements

- IntelliJ IDEA 2023.2+ (build 232+) or any JetBrains IDE
- JDK 17+

## Configuration

Settings → Tools → LogParser Pro

| Setting | Description | Default |
|---------|-------------|---------|
| Max lines to parse | Maximum number of lines to load from a file | 100,000 |
| Default filter level | Initial log level filter | ALL |
| Show line numbers | Display line numbers in the editor | true |
| Auto-refresh on file change | Reload when the log file changes | false |
| Date format pattern | Pattern for timestamp parsing | yyyy-MM-dd HH:mm:ss.SSS |
| Custom correlation ID patterns | Comma-separated regex patterns | (empty) |
| Use theme-aware colours | Adapt colours to light/dark theme | true |
| Wrap long lines | Word wrap in the log editor | false |
| Font size for log viewer | Font size in points | 12 |
| Export format preference | Default export format | CSV |

## Usage

### Open a Log File
Tools → LogParser Pro → **Open Log File** — select any `.log` or `.out` file.

### Analyze a Log File
Tools → LogParser Pro → **Analyze Log** — run full analysis (statistics + anomaly detection) on the current file.

### Track Correlation IDs
Tools → LogParser Pro → **Track Transaction ID** — enter a correlation ID to search the current file.

### Find Correlation IDs
Select text in the editor → right-click → **Find Correlation ID** to extract and search for correlation identifiers.

### Extract Patterns
Select text in the editor → right-click → **Extract Pattern** to generate a regex with named capture groups.

### Tool Window
Open the **LogParser Pro** tool window (bottom panel) for tabbed access to:
- **Log Viewer** — Quick file open and recent files
- **Search** — Regex, fuzzy, and simple text search
- **Correlation** — Enter correlation ID, see timeline and matching entries
- **Statistics** — Level distribution, top loggers, entries per hour

## Building from Source

```bash
./gradlew buildPlugin
```

## Testing

```bash
./gradlew test
```

## License

Apache License 2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE). Copyright 2026 Jakub Jirák.

## Free & open source

Since version 2026.8.0 the plugin is completely free — every feature is unlocked and no license is needed. The source code is open source under the [Apache License 2.0](LICENSE).

## Contributing

Issues and pull requests are welcome at https://github.com/JirakJ/logparser-premium-plugin.
