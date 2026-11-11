# LogParser Pro

[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)

Advanced log file analysis plugin for JetBrains IDEs with colour-coded log levels, filtering, pattern matching, correlation tracking, and statistical anomaly detection.

## Features

- **Colour-coded log levels** — ERROR, WARN, INFO, DEBUG, TRACE highlighted for instant visibility
- **Syntax highlighting** — Full language-level syntax highlighting in the text editor
- **Dedicated log editor** — Custom editor tab for `.log` and `.out` files with 6-column table view (Line, Timestamp, Level, Thread, Logger, Message)
- **Level toggle buttons** — Filter entries by severity with toggle buttons for each level
- **Keyword search** — Live filtering across log entries with search field
- **Pattern library** — Auto-detection of Spring Boot, Node.js, Python, and Java log formats
- **Stack trace folding** — Consecutive stack trace lines are folded into the parent entry
- **Regex pattern extraction** — Generate regex patterns from selected text (UUIDs, IPs, timestamps, numbers)
- **Settings** — Max lines, font size, theme-aware level colours, editor export format
- **Correlation ID tracking** — Track request flows across log entries by correlation/trace ID
- **Advanced search** — Regex, fuzzy (Levenshtein distance), and simple text search
- **Log classification** — Keyword-based categorisation of entries into database, network, security, performance, business logic, and system
- **Anomaly detection** — Statistical anomaly scoring based on feature-space distance
- **Pattern discovery** — Automatic grouping of similar messages with normalisation
- **Statistics dashboard** — Level distribution, top 10 loggers, entries per hour, error/warning rates
- **Export** — Editor Export button writes the filtered rows to CSV, JSON or plain text; Tools → LogParser Pro → Export Log… writes CSV, JSON or a standalone HTML report (CSV cells are protected against spreadsheet formula injection)
- **Context menu** — Right-click a row: Copy Line, Copy Message, Track Correlation ID, Go to Line (opens the log in the text editor at that line)
- **Tool window tabs** — Log Viewer, Search, Correlation, and Statistics tabs

## Installation

1. **JetBrains Marketplace**: Settings → Plugins → Marketplace → Search "LogParser Pro"
2. **Manual**: Download from [Releases](https://github.com/JirakJ/logparser-premium-plugin/releases) → Settings → Plugins → ⚙️ → Install from disk

## Requirements

- Any JetBrains IDE 2025.1 or newer (build 251+)

## Configuration

Settings → Tools → LogParser Pro

| Setting | Description | Default |
|---------|-------------|---------|
| Max lines to parse | Maximum number of lines the log editor loads from a file | 100,000 |
| Font size for log viewer | Font size of the log editor table, in points | 12 |
| Editor export format | Format used by the log editor's Export button (CSV, JSON, Plain Text) | CSV |
| Use theme-aware colours | Colour levels with the current theme's palette instead of fixed colours | true |

## Usage

### Open a Log File
Open any `.log` or `.out` file and switch to the **LogParser** editor tab (next to the text editor) for the
table view with level toggles, search and export.

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
