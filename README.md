# LogParser Pro - IntelliJ IDEA Plugin

<div align="center">

![LogParser Pro](https://img.shields.io/badge/LogParser-Pro-E74C3C?style=for-the-badge)
![Version](https://img.shields.io/badge/version-1.0.0--SNAPSHOT-34495E?style=for-the-badge)
![License](https://img.shields.io/badge/license-Proprietary-F39C12?style=for-the-badge)

**Professional Log Analysis & Correlation Tracking for JetBrains IDEs**

[Features](#features) • [Installation](#installation) • [Pricing](#pricing) • [Documentation](#documentation)

</div>

---

## 🎯 Overview

LogParser Pro is a premium IntelliJ IDEA plugin that revolutionizes how developers debug distributed systems. Track transaction flows across microservices, correlate logs from multiple sources, and identify issues faster than ever before.

**The Problem:** Debugging distributed systems requires jumping between kubectl logs, SSH sessions, grep commands, and multiple terminal windows. Finding a single transaction across 20+ microservices takes hours.

**The Solution:** LogParser Pro aggregates logs from Kubernetes, Docker, SSH, and local files into one unified view with intelligent correlation ID tracking. See your entire distributed transaction in seconds, not hours.

---

## ✨ Features

### 🆓 **FREE Features** (Community Edition)

- ✅ **Syntax Highlighting** - JSON, plaintext, Apache/Nginx, common formats
- ✅ **Log Level Filtering** - ERROR, WARN, INFO, DEBUG with color coding
- ✅ **Keyword Search** - Fast string matching with highlighting
- ✅ **Auto-detect Patterns** - Spring Boot, Node.js, Python, Java logs
- ✅ **Stack Trace Folding** - Collapse exception traces for readability
- ✅ **Export to TXT** - Save filtered results (max 1,000 lines)

### 💎 **PREMIUM Features** ($79/year)

- 🔥 **Transaction/Correlation ID Tracking** - Cross-pod log correlation
- ☸️ **Kubernetes & Docker Integration** - Native kubectl logs support
- 🔴 **Real-time Multi-Source Monitoring** - Live tail 10+ sources
- 🤖 **AI-powered Pattern Detection** - ML-based anomaly detection
- 📊 **Advanced Analytics & Visualization** - Dashboards, heatmaps, metrics
- 👥 **Team Collaboration** - Shared queries, annotations
- ⚡ **Unlimited Performance** - Handle GB+ files effortlessly
- 🔗 **Integration Ecosystem** - JIRA, Slack, API access

---

## 💰 Pricing

| Plan | Price | Features |
|------|-------|----------|
| **Free** | $0 | Basic log viewing & filtering |
| **Premium** | $79/year | All features + 30-day FREE trial |
| **Team (5+)** | $59/seat/year | 25% discount + shared queries |
| **Enterprise** | Custom | On-premise + SLA + training |

**Start your FREE 30-day trial today - no credit card required!**

---

## 📦 Installation

### Via JetBrains Marketplace (Coming Soon)

1. Open IntelliJ IDEA
2. **Settings/Preferences** → **Plugins**
3. Search for "**LogParser Pro**"
4. Click **Install** → Restart IDE

### Manual Installation (Development)

```bash
git clone https://github.com/yourusername/logparser.git
cd logparser
./gradlew buildPlugin
```

---

## 🚀 Quick Start

### 1. Open a Log File
- **Menu:** `LogParser Pro` → `Open Log File`
- Select any `.log`, `.txt`, or `.out` file

### 2. Start Premium Trial
- **Menu:** `LogParser Pro` → `Manage License`
- Click **Start 30-Day FREE Trial**

### 3. Track Correlation IDs (Premium)
- Select correlation ID in editor
- Right-click → `Find Correlation ID (Premium)`
- View unified transaction timeline

### 4. Connect to Kubernetes (Premium)
- **Menu:** `LogParser Pro` → `Connect Log Source`
- Select Kubernetes Pod
- Live tail with correlation tracking

---

## 🏗️ Development

### Prerequisites
- JDK 17+
- IntelliJ IDEA 2023.2+
- Gradle 8.0+

### Build Commands

```bash
# Build plugin
./gradlew build

# Run in development IDE
./gradlew runIde

# Run tests
./gradlew test

# Build distributable plugin
./gradlew buildPlugin
```

---

## 🎨 Technology Stack

- **IntelliJ Platform SDK** - Plugin framework
- **Kotlin/Java** - Core implementation
- **Apache Lucene** - High-performance indexing
- **RxJava** - Reactive streaming
- **Isolation Forest** - ML anomaly detection

---

## 🤝 Why LogParser Pro?

### vs. Datadog
- **10x cheaper** ($79/year vs $100+/month)
- **Offline analysis** (data stays local)
- **Native IDE integration**

### vs. Splunk
- **Instant setup** (no infrastructure)
- **Easy learning curve**
- **Developer-focused UI**

### vs. CLI Tools (grep/awk)
- **Unified multi-source view**
- **Automatic correlation tracking**
- **Visual timelines & heatmaps**

---

## 📞 Support

- 📧 **Email:** support@logparser.dev
- 💬 **Discord:** [Join Community](https://discord.gg/logparser)
- 📚 **Docs:** [docs.logparser.dev](https://docs.logparser.dev)
- 🐛 **Issues:** [GitHub Issues](https://github.com/yourusername/logparser/issues)

---

## 🗺️ Roadmap

### ✅ Phase 1 (MVP) - Q1 2024
- Free tier with basic features
- Premium licensing system
- Correlation ID tracking
- Kubernetes integration

### 🔄 Phase 2 - Q2 2024
- AI pattern recognition
- Analytics dashboard
- JIRA/Slack integrations

### 📋 Phase 3 - Q3 2024
- VSCode extension
- Cloud storage support
- Custom ML models

---

## 📄 License

**LogParser Pro** is proprietary software with a freemium model:
- **Free Tier:** Unrestricted use with feature limitations
- **Premium Tier:** Annual subscription required

See [LICENSE](LICENSE) for details.

---

## 🌟 Market Opportunity

**Target Market:** 10M+ JetBrains IDE users worldwide

**Revenue Potential:**
- Conservative: 5,000 users × $79 = **$395k ARR**
- Realistic: 15,000 users × $79 = **$1.18M ARR**
- Optimistic: 30,000 users × $79 = **$2.37M ARR**

**Competitive Gap:** No premium log parser with correlation tracking exists for JetBrains IDEs!

---

<div align="center">

**Built with ❤️ by developers, for developers**

[Get Started](#installation) • [Documentation](#documentation) • [Pricing](#pricing)

Made with the cyber-industrial color palette: #E74C3C • #34495E • #F39C12

</div>
