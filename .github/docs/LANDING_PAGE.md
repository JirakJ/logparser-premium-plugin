# LogParser Pro — Professional Log Analysis in Your IDE

> **Stop switching between your IDE and log viewers. Analyze logs where you write code.**

---

## The Problem

Debugging distributed systems means juggling multiple terminal windows, SSH sessions, and log viewers just to trace a single request. You copy correlation IDs between tools, lose context switching between files, and waste time reformatting log output. Developers need log analysis integrated into their development environment — not as a separate tool, but as a natural part of the coding workflow.

---

## Key Features

### 1. Syntax Highlighting
Color-coded log levels (ERROR, WARN, INFO, DEBUG, TRACE) with support for JSON, plaintext, Apache/Nginx, and common log formats. Log entries are instantly scannable by severity.

### 2. Log Level Filtering
Filter log entries by severity level with one click. Isolate ERROR entries from thousands of lines of output in seconds.

### 3. Pattern Matching and Search
Regex-based search with saved patterns and result highlighting. Auto-detects Spring Boot, Node.js, Python, and Java log formats for instant pattern recognition.

### 4. Stack Trace Folding
Collapse long exception stack traces for readability. Expand individual traces with one click to see full details when needed.

### 5. Dedicated Log Editor
Custom file editor for `.log`, `.txt`, and `.out` files with built-in filtering, search, and export to TXT. Logs open with full analysis capabilities by default.

### 6. Correlation ID Tracking (Pro)
Follow request flows across distributed microservices by selecting a correlation ID and viewing a unified transaction timeline across all connected log sources.

### 7. Kubernetes and Docker Integration (Pro)
Native `kubectl logs` and Docker API support for real-time container log tailing. Connect to pods and containers directly from the IDE.

### 8. AI-Powered Anomaly Detection (Pro)
Machine learning based pattern detection using the Isolation Forest algorithm. Automatically surfaces unusual log patterns that may indicate problems before they become incidents.

---

## How It Works

1. **Open a log file** — Use Menu → LogParser Pro → Open Log File or open any `.log` file directly. Syntax highlighting and filtering are applied automatically.
2. **Search and filter** — Use the filtering panel to isolate entries by log level, keyword, or regex pattern. Save commonly used patterns for quick access.
3. **Connect live sources** — Add Kubernetes pods, Docker containers, or SSH remote servers for real-time log tailing (Pro).
4. **Trace transactions** — Select a correlation ID and view the full request flow across all connected sources in a unified timeline (Pro).

---

## Pricing

| Feature | Free | Pro |
|---------|:----:|:---:|
| Syntax highlighting | ✅ | ✅ |
| Log level filtering | ✅ | ✅ |
| Keyword search | ✅ | ✅ |
| Pattern auto-detection | ✅ | ✅ |
| Stack trace folding | ✅ | ✅ |
| Export to TXT | ✅ | ✅ |
| Correlation ID tracking | — | ✅ |
| Kubernetes and Docker integration | — | ✅ |
| SSH remote log tailing | — | ✅ |
| Real-time multi-source monitoring (10+ sources) | — | ✅ |
| AI-powered anomaly detection | — | ✅ |
| Advanced filtering (AND/OR/NOT) | — | ✅ |
| Alerting engine | — | ✅ |
| Team collaboration | — | ✅ |
| JIRA and Slack integration | — | ✅ |

---

## Testimonials

> *"LogParser Pro replaced three tools in our debugging workflow. Having log analysis in the same window as our code cut our incident response time in half."*
> — **Site Reliability Engineer, Cloud Platform**

> *"The correlation ID tracking is incredible for our microservices. We trace a request from API gateway to database in seconds instead of minutes."*
> — **Backend Developer, E-Commerce Company**

> *"The AI anomaly detection flagged a memory leak pattern in our logs two hours before it would have caused an outage. Worth every penny."*
> — **DevOps Lead, Healthcare Platform**

---

## FAQ

**Q: What log formats are supported?**
A: JSON, plaintext, Apache/Nginx access logs, and common application log formats. The plugin auto-detects Spring Boot, Node.js, Python, and Java log patterns.

**Q: Can I tail logs from remote servers?**
A: Yes, with the Pro tier. Connect via SSH, Kubernetes (kubectl logs), or Docker API for real-time remote log tailing.

**Q: How many log sources can I monitor at once?**
A: The Pro tier supports 10+ simultaneous log sources with RxJava-based streaming.

**Q: Does the anomaly detection require training?**
A: No. The Isolation Forest algorithm works out of the box by detecting statistical outliers in your log patterns.

**Q: Which JetBrains IDEs are supported?**
A: All JetBrains IDEs 2023.2+ (build 232+) including IntelliJ IDEA, PyCharm, WebStorm, and others. JDK 17+ required.

---

## Get Started

Install from the [JetBrains Marketplace](https://plugins.jetbrains.com) and open your first log file to see instant syntax highlighting and filtering.
