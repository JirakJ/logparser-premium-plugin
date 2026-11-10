# Changelog

All notable changes to LogParser Pro will be documented in this file.

## [2026.8.0] - 2026-11-10

### Changed
- The plugin is now completely free — all features unlocked, no license needed.
- Source code is now open source under Apache-2.0: https://github.com/JirakJ/logparser-premium-plugin

## [2026.4.1] - 2026-04-22

### Added
- **Gzip support** — Transparent reading of `*.log.gz` files using either the filename or a gzip magic-number sniff; does not register `.gz` as a file type.
- **Structured log parser** — Detects and extracts JSON and logfmt entries into typed fields (timestamp, level, logger, message plus arbitrary extra fields), with leading-anchor detection to avoid false positives in free-form text.
- **Exception grouping** — `Tools → LogParser Pro → Group Exceptions` buckets stack traces by a normalised signature (lambda/enhancer suffixes, hex addresses, line numbers, numeric IDs all collapsed), preserves sample line numbers, first/last seen timestamps, and root cause extracted from `Caused by:` chains.
- **Error timeline** — `Tools → LogParser Pro → Show Error Timeline` renders minute-granularity histograms with z-score spike detection.
- **Log diff** — `Tools → LogParser Pro → Diff With Another Log…` compares two logs by normalised message signature.
- **Secret redaction** — `Tools → LogParser Pro → Redact Secrets to File…` writes a redacted copy masking emails, JWTs, Bearer tokens, AWS/Stripe keys, Luhn-verified credit cards, UUIDs, IPs and password assignments (custom patterns supported).
- **Grok pattern compiler** — Elastic Grok subset → native `Regex` with cycle detection, duplicate-name detection and expansion limits (~35 built-in patterns).
- **Safer export** — `Tools → LogParser Pro → Export Log…` writes CSV (RFC-4180 with spreadsheet-formula-injection mitigation), JSON, or a self-contained HTML report with all user content HTML-escaped.
- **Live tail reader** — Byte-offset incremental reader with UTF-8-safe partial-line buffering, CRLF handling and truncation detection.

### Changed
- **Plugin description** — Updated feature list with the new v2026.4.1 capabilities.

### Compatibility
- **sinceBuild raised to 251** (IntelliJ 2025.1+). Compile target bumped to 2025.1 so that all Platform API usages resolve to non-deprecated overloads. Verifier: 0 deprecated / 0 internal / 0 experimental / 0 scheduled-for-removal against IC-2025.1.3 and IC-2025.2.4.

## [2026.3.12] - 2026-03-28

### Changed
- **Removed custom file dialogs** — Plugin no longer opens its own file chooser; works purely on files already open in the IDE
- **LogViewerPanel rewrite** — Tool window now auto-tracks `.log`/`.out` files opened in the editor via `FileEditorManagerListener`
- **Removed OpenLogFileAction** — No longer needed; users open log files through standard IDE mechanisms (File → Open, drag & drop, project tree)

## [2026.3.11] - 2026-03-28

### Fixed
- **Removed deprecated API** — Replaced `FileChooserDialog.choose(VirtualFile, Project)` with `FileChooser.chooseFiles()` to fix scheduled-for-removal API warnings on IntelliJ 2026.1+

## [2026.3.10] - 2026-03-27

### Added
- **Freemium licensing** — Added `product-descriptor` for JetBrains Marketplace freemium model (code: `PLOGPARSERPRO`)

## [2026.3.9] - 2026-03

### Fixed
- **Plugin verification failure** — Removed bundled `kotlin-stdlib` and `annotations` that conflicted with IDE-provided versions, causing "archive cannot be extracted" on IntelliJ 2023.2
- **Missing toolWindow icon** — Replaced nonexistent `/icons/logparser.svg` with built-in `AllIcons.FileTypes.Text`
- **Plugin verifier configuration** — Now verifies against IntelliJ 2023.2 and 2024.3 explicitly

## [2026.3.8] - 2026-03

### Fixed
- **Critical: LogLexer infinite loop** — Fixed `advance()` method that produced zero-length tokens due to wrong assignment order, causing the IDE to fail when opening `.log` and `.out` files

## [2026.3.7] - 2026-03

### Changed
- **Complete Rewrite** — All source code rewritten under `com.jakubjirak.logparser.pro` package
- **LanguageFileType** — Proper `LogLanguage` + `LanguageFileType` + `LogParserDefinition` integration
- **Syntax Highlighting** — Full colour-coded log levels in the text editor via `LogSyntaxHighlighter`
- **Tools Menu** — Actions moved from main menu to **Tools → LogParser Pro**
- **Rich Settings** — 10 configurable options: max lines, font size, date format, export format, theme colours, correlation patterns, and more
- **Enhanced Editor** — 6-column table (Line, Timestamp, Level, Thread, Logger, Message) with level toggle buttons
- **Export** — Export filtered entries to CSV, JSON, or plain text
- **Context Menu** — Right-click: Copy Line, Copy Message, Track Correlation ID, Go to Line
- **Tool Window Tabs** — Log Viewer, Search, Correlation, and Statistics tabs
- **Statistics Dashboard** — Level distribution bar chart, top 10 loggers, entries per hour
- **Analyze Log Action** — One-click full analysis report with anomaly detection
- **File Type** — Only accepts `.log` and `.out` files (removed `.txt` to avoid hijacking text files)
- **No Kubernetes Dependency** — Removed optional kubernetes-support.xml

### Technical
- 8 test classes with 100+ tests (JUnit 5)
- Full KDoc documentation on all public classes and methods
- License headers on all source files
- Package: `com.jakubjirak.logparser.pro`
- IntelliJ Platform SDK 2024.3, Kotlin 2.1.0, JDK 17

## [2026.3.2] - 2026-03

### Changed
- **All Features Free** — Removed all licensing, premium gating, and trial logic; every feature is now available to all users at no cost
- Removed LicenseManager, ManageLicenseAction, UpgradePremiumAction, and ConnectSourceAction
- Removed product-descriptor from plugin.xml
- Simplified tool window UI (removed trial/upgrade buttons and license status)
- Correlation tracking and advanced search no longer require a license
- Removed PREMIUM_FEATURES.md, PRICING.md, and MARKETPLACE_LISTING.md

## [2026.3.1] - 2026-03

### Added
- **Real Log Editor** — Full table-based log viewer with colour-coded levels, keyword search, level filtering, and entry-count status bar
- **Working Settings Panel** — Configurable max lines to parse, default filter level, line numbers, and auto-refresh (Settings → Tools → LogParser Pro)
- **Pattern Extraction** — Generate regex from selected text with automatic UUID, timestamp, IP, and number recognition
- **Correlation Tracking** — Search and track correlation IDs across log entries with duration and error reporting
- **Advanced Search** — Regex, fuzzy (Levenshtein distance), and simple substring search modes
- **Log Indexing** — Multi-threaded log file indexing across four dimensions (word, timestamp, level, thread) for fast lookups
- **ML Classification** — Keyword-based classification into six categories (database, network, security, performance, business_logic, system)
- **Anomaly Detection** — Statistical anomaly scoring with Euclidean distance in feature space
- **Pattern Discovery** — Automatic grouping of similar messages with normalisation (numbers, UUIDs, IPs, emails)
- **Feedback System** — Non-intrusive feedback prompt after 7 days of usage with dismiss option

### Changed
- Removed 39 stub/scaffold files that had no real implementation
- Merged duplicate LicenseManager classes into a single `core.LicenseManager`
- Replaced `Desktop.browse()` with IntelliJ `BrowserUtil.browse()` for cross-platform compatibility
- Replaced all `throw IllegalStateException` with `check()`/`checkNotNull()` per Kotlin conventions
- Refactored `LogEntry.matches()` to respect detekt ReturnCount limits
- Refactored `AdvancedSearchEngine.search()` to use `mapNotNull` instead of loop with multiple `continue`

### Fixed
- `OpenLogFileAction` now actually opens the selected file in the editor (was a no-op)
- `TrackCorrelationAction` now performs real correlation search instead of showing "under development"
- `FindCorrelationIdAction` now extracts and displays correlation IDs from selected text
- `ExtractPatternAction` now generates real regex patterns instead of showing "coming soon"
- `LogFileEditor` now displays parsed log entries instead of "Coming Soon!" placeholder
- `LogParserConfigurable` now has real settings instead of "Settings coming soon!" text
- `PatternLibrary.isStackTraceLine()` no longer matches any line containing "Exception"
- `CorrelationTracker.hasErrors` now checks `LogLevel` enum directly instead of string comparison
- `LocalFileSource.tailFollow()` now parses new lines into `LogEntry` objects

### Removed
- Kubernetes log source (stub)
- Docker log source (stub)
- SSH log source (stub)
- Team Collaboration (stub)
- Alerting Engine (stub)
- REST API Server (stub)
- Cloud Storage (stub)
- Analytics Dashboard (stub)
- 21 premium stub files (audit, batch, bookmarks, export, profiler, security, themes, etc.)

### Technical
- 146 unit tests with JUnit 5
- Zero detekt violations (maxIssues: 0)
- Full KDoc documentation on all public classes and methods
- License headers on all source files
- IntelliJ Platform SDK 2024.3, Kotlin 2.1.0, JDK 17
