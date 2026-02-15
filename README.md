# LogParser Pro

Advanced log file analysis and monitoring plugin for JetBrains IDEs featuring syntax highlighting, real-time log tailing, pattern matching, correlation tracking, and AI-powered anomaly detection for distributed systems.

## Features

- **Syntax highlighting** — color-coded support for JSON, plaintext, Apache/Nginx, and common log formats
- **Log level filtering** — filter by ERROR, WARN, INFO, DEBUG with color coding
- **Keyword search** — fast string matching with result highlighting
- **Auto-detect patterns** — recognizes Spring Boot, Node.js, Python, and Java log formats
- **Stack trace folding** — collapse exception traces for readability
- **Custom log file editor** — dedicated editor for `.log`, `.txt`, and `.out` files
- **Export to TXT** — save filtered results
- **Transaction/Correlation ID tracking** — cross-pod log correlation across microservices (Premium)
- **Kubernetes & Docker integration** — native `kubectl logs` and Docker API support (Premium)
- **SSH remote log tailing** — connect to remote servers (Premium)
- **Real-time multi-source monitoring** — live tail 10+ sources simultaneously with RxJava streaming (Premium)
- **AI-powered anomaly detection** — ML-based pattern detection using Isolation Forest algorithm (Premium)
- **Advanced analytics & visualization** — dashboards, heatmaps, and metrics (Premium)
- **Advanced filtering engine** — composite filters with AND/OR/NOT logic (Premium)
- **Alerting engine** — real-time alerts based on configurable patterns (Premium)
- **Team collaboration** — shared queries and annotations (Premium)
- **Integration ecosystem** — JIRA, Slack, and REST API access (Premium)

## Installation

1. **JetBrains Marketplace**: Settings → Plugins → Marketplace → Search "LogParser Pro"
2. **Manual**: Download from [Releases](https://github.com/JirakJ/logparser-premium-plugin/releases) → Settings → Plugins → ⚙️ → Install from disk

## Requirements

- IntelliJ IDEA 2023.2+ (build 232+)
- Any JetBrains IDE (IntelliJ, WebStorm, PyCharm, etc.)
- JDK 17+

## Configuration

Settings → Tools → LogParser Pro

Configure log format detection, filtering preferences, and integration connections.

## Usage

### Open a Log File
Menu → LogParser Pro → **Open Log File** — select any `.log`, `.txt`, or `.out` file to open it in the dedicated log editor with syntax highlighting and filtering.

### Track Correlation IDs (Premium)
Select a correlation ID in the editor → right-click → **Find Correlation ID** to view a unified transaction timeline across all connected log sources.

### Connect Log Sources (Premium)
Menu → LogParser Pro → **Connect Log Source** to add Kubernetes pods, Docker containers, or SSH remote servers for real-time log tailing.

### Extract Patterns
Right-click in the log editor → **Extract Pattern** to save reusable pattern definitions for common log formats.

## Building from Source

```bash
./gradlew buildPlugin
```

## License

Copyright © 2026 Jakub Jirák. All rights reserved.
