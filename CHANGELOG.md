# Changelog

All notable changes to LogParser Pro will be documented in this file.

## [Unreleased]

### Added
- Initial project setup
- Free tier features
- Premium licensing system
- Correlation ID tracking foundation
- Kubernetes log source integration
- Basic log parsing engine

## [1.0.0-SNAPSHOT] - 2024-01-XX

### Added
- 🆓 **Free Features:**
  - Syntax highlighting for common log formats
  - Log level filtering (ERROR, WARN, INFO, DEBUG)
  - Keyword search with highlighting
  - Auto-detect patterns (Spring Boot, Node.js, Python)
  - Basic timestamp parsing
  - Stack trace folding
  - Thread name extraction
  - Export to TXT (max 1,000 lines)

- 💎 **Premium Features:**
  - Transaction/Correlation ID tracking
  - Multi-source log aggregation
  - Kubernetes pod integration (kubectl logs)
  - Real-time live tailing
  - Advanced pattern recognition
  - License management system
  - 30-day free trial

### Technical
- Gradle-based build system
- Kotlin/Java implementation
- Apache Lucene indexing
- RxJava reactive streams
- IntelliJ Platform SDK 2023.2+

### Plugin Structure
- Core parsing engine
- UI components (Tool Window, Actions, Editors)
- License manager with trial support
- Log source abstraction layer

---

## Release Schedule

- **Q1 2024:** MVP launch (free + premium tier)
- **Q2 2024:** AI features + integrations
- **Q3 2024:** VSCode port + cloud support
- **Q4 2024:** Enterprise features

---

### Version Format

We use [Semantic Versioning](https://semver.org/):
- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes

Example: `1.2.3` = Major 1, Minor 2, Patch 3
