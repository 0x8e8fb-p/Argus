# Graph Report - NexusBlock-AndroidTV  (2026-05-22)

## Corpus Check
- 72 files · ~40,663 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 656 nodes · 805 edges · 63 communities (29 shown, 34 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 56 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `88611914`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- [[_COMMUNITY_UI Screens & Compose Components|UI Screens & Compose Components]]
- [[_COMMUNITY_VPN DNS Update & Filtering|VPN DNS Update & Filtering]]
- [[_COMMUNITY_Network Packet Routing|Network Packet Routing]]
- [[_COMMUNITY_Blocklist Rule Engine|Blocklist Rule Engine]]
- [[_COMMUNITY_Ad Skip Orchestrator & Accessibility|Ad Skip Orchestrator & Accessibility]]
- [[_COMMUNITY_VPN Service Lifecycle|VPN Service Lifecycle]]
- [[_COMMUNITY_Architecture & Dependencies|Architecture & Dependencies]]
- [[_COMMUNITY_Configuration & Settings|Configuration & Settings]]
- [[_COMMUNITY_Proxy Dependencies & Server|Proxy Dependencies & Server]]
- [[_COMMUNITY_Content Transformers|Content Transformers]]
- [[_COMMUNITY_Domain DAO & Repository|Domain DAO & Repository]]
- [[_COMMUNITY_Native TUN Bridge|Native TUN Bridge]]
- [[_COMMUNITY_Certificate Management|Certificate Management]]
- [[_COMMUNITY_MITM SSL Handling|MITM SSL Handling]]
- [[_COMMUNITY_Transformer Tests|Transformer Tests]]
- [[_COMMUNITY_Connection Tracker|Connection Tracker]]
- [[_COMMUNITY_QUIC Inspector Tests|QUIC Inspector Tests]]
- [[_COMMUNITY_Custom Rule DAO|Custom Rule DAO]]
- [[_COMMUNITY_Blocked Event Stats|Blocked Event Stats]]
- [[_COMMUNITY_QuickJS Transformer|QuickJS Transformer]]
- [[_COMMUNITY_Custom Rule Repository|Custom Rule Repository]]
- [[_COMMUNITY_App Strategy & Routing Tests|App Strategy & Routing Tests]]
- [[_COMMUNITY_tun2socks Native Stack|tun2socks Native Stack]]
- [[_COMMUNITY_Dashboard ViewModel|Dashboard ViewModel]]
- [[_COMMUNITY_Blocked Event DAO|Blocked Event DAO]]
- [[_COMMUNITY_YouTube Transformer Tests|YouTube Transformer Tests]]
- [[_COMMUNITY_Audio Ad Detection|Audio Ad Detection]]
- [[_COMMUNITY_LiteRT Ad Classifier|LiteRT Ad Classifier]]
- [[_COMMUNITY_Blocklist Parsers|Blocklist Parsers]]
- [[_COMMUNITY_Settings ViewModel|Settings ViewModel]]
- [[_COMMUNITY_Custom Rules UI|Custom Rules UI]]
- [[_COMMUNITY_Spotify Transformer Tests|Spotify Transformer Tests]]
- [[_COMMUNITY_Strategy Router|Strategy Router]]
- [[_COMMUNITY_Hotstar Transformer Rules|Hotstar Transformer Rules]]
- [[_COMMUNITY_Database DI Module|Database DI Module]]
- [[_COMMUNITY_QUIC Inspector|QUIC Inspector]]
- [[_COMMUNITY_Strategy Matrix|Strategy Matrix]]
- [[_COMMUNITY_Firewall ViewModel|Firewall ViewModel]]
- [[_COMMUNITY_App DI Module|App DI Module]]
- [[_COMMUNITY_Blocklist ViewModel|Blocklist ViewModel]]
- [[_COMMUNITY_Logs ViewModel|Logs ViewModel]]
- [[_COMMUNITY_DataStore & Room|DataStore & Room]]
- [[_COMMUNITY_Go Mobile Build|Go Mobile Build]]
- [[_COMMUNITY_Spotify JS Transformer|Spotify JS Transformer]]
- [[_COMMUNITY_YouTube JS Transformer|YouTube JS Transformer]]
- [[_COMMUNITY_App Build Config|App Build Config]]
- [[_COMMUNITY_Project Settings|Project Settings]]
- [[_COMMUNITY_Root Build Config|Root Build Config]]
- [[_COMMUNITY_Typography|Typography]]
- [[_COMMUNITY_Gradle Wrapper|Gradle Wrapper]]
- [[_COMMUNITY_Android Plugin|Android Plugin]]
- [[_COMMUNITY_Kotlin Plugin|Kotlin Plugin]]
- [[_COMMUNITY_Hilt Plugin|Hilt Plugin]]
- [[_COMMUNITY_KSP Plugin|KSP Plugin]]
- [[_COMMUNITY_Compose Plugin|Compose Plugin]]
- [[_COMMUNITY_DNS Upstream Manager|DNS Upstream Manager]]

## God Nodes (most connected - your core abstractions)
1. `DnsFilterEngine` - 22 edges
2. `RuleEngine` - 21 edges
3. `FocusPanel()` - 16 edges
4. `ARGUS — Rethink & Redesign Document` - 15 edges
5. `NexusVpnService` - 14 edges
6. `BlockedDomainDao` - 14 edges
7. `PacketRouter` - 14 edges
8. `ConnectionTracker` - 13 edges
9. `NexusBlock — Device-Wide Ad Blocker for Android TV` - 13 edges
10. `BlockedDomain` - 12 edges

## Surprising Connections (you probably didn't know these)
- `NexusBlockApp()` --calls--> `NexusBackground()`  [INFERRED]
  app/src/main/java/com/nexusblock/ui/MainActivity.kt → app/src/main/java/com/nexusblock/ui/components/NexusTvComponents.kt
- `NexusBlockApp()` --calls--> `NexusNavigationRail()`  [INFERRED]
  app/src/main/java/com/nexusblock/ui/MainActivity.kt → app/src/main/java/com/nexusblock/ui/components/NexusTvComponents.kt
- `NexusBlockApp()` --calls--> `DashboardScreen()`  [INFERRED]
  app/src/main/java/com/nexusblock/ui/MainActivity.kt → app/src/main/java/com/nexusblock/ui/screens/DashboardScreen.kt
- `NexusBlockApp()` --calls--> `BlocklistScreen()`  [INFERRED]
  app/src/main/java/com/nexusblock/ui/MainActivity.kt → app/src/main/java/com/nexusblock/ui/screens/BlocklistScreen.kt
- `NexusBlockApp()` --calls--> `CustomRulesScreen()`  [INFERRED]
  app/src/main/java/com/nexusblock/ui/MainActivity.kt → app/src/main/java/com/nexusblock/ui/screens/CustomRulesScreen.kt

## Communities (63 total, 34 thin omitted)

### Community 0 - "UI Screens & Compose Components"
Cohesion: 0.09
Nodes (36): AnimatedStateText(), FocusPanel(), MetricTile(), NavRailItem(), NexusBackground(), NexusNavigationRail(), NexusScreenHeader(), RailItem (+28 more)

### Community 1 - "VPN DNS Update & Filtering"
Cohesion: 0.06
Nodes (9): CidrMatcher, DomainTrie, RuleEngine, TrieNode, RuleEngineTest, BlockedDomain, BlocklistRepository, BuiltInBlockRules (+1 more)

### Community 3 - "Blocklist Rule Engine"
Cohesion: 0.04
Nodes (45): 10. Key Engineering Decisions Documented, 11. Threat Model & Safety, 12. Legal / Distribution, 13. Next Step — Awaiting Your Approval, 1. Executive Summary: Why the Original Spec Was Wrong, 2. What the Stash/Surge Reddit Post Actually Reveals, 3. What NexusBlock Already Does Well (Preserve), 4.1 Remove: Go + Rust Native Components (+37 more)

### Community 4 - "Ad Skip Orchestrator & Accessibility"
Cohesion: 0.19
Nodes (5): bytesToIp(), calculateChecksum(), calculatePseudoChecksum(), PacketRouter, RouterStats

### Community 9 - "Content Transformers"
Cohesion: 0.23
Nodes (3): start(), stop(), VpnWatchdogService

### Community 19 - "QuickJS Transformer"
Cohesion: 0.36
Nodes (4): BlocklistUpdateWorker, cancel(), runNow(), schedule()

### Community 31 - "Spotify Transformer Tests"
Cohesion: 0.60
Nodes (3): BlocklistFormat, BlocklistSourceDefinition, BlocklistSources

### Community 32 - "Strategy Router"
Cohesion: 0.60
Nodes (3): description(), displayName(), FirewallMode

### Community 40 - "Blocklist ViewModel"
Cohesion: 0.05
Nodes (40): 1. Core Architecture & System Overview, 1. `NexusVpnService.kt`, 1. `PacketRouter.kt`, 2. `DnsFilterEngine.kt`, 2. Module-by-Module Technical Breakdown, 2. `VpnWatchdogService.kt` & `BootReceiver.kt`, 3. Key Network & Process Flows, 3. `RuleEngine.kt` & `BloomFilter.kt` (+32 more)

### Community 41 - "Logs ViewModel"
Cohesion: 0.10
Nodes (19): 10. Subscription / Monetization, 1. Architecture Overview, 2. Native VPN (strongSwan), 3. YouTube Strategy (CRITICAL INSIGHT), 4. Blocklist Infrastructure, 5. DNS/Filtering Approach, 6. Certificate Management, 7. In-App Browser (+11 more)

### Community 42 - "DataStore & Room"
Cohesion: 0.11
Nodes (18): Argus Ad Blocker — DNS + SNI + MITM API Rewriter MVP, Build Verification ✅, code:bash (./gradlew clean assembleDebug), Defect Triage Categories, Regression Tests, Required Accounts, Required Hardware, Sign-Off Checklist (+10 more)

### Community 43 - "Go Mobile Build"
Cohesion: 0.18
Nodes (11): 10. Background Resilience, 1. NexusVpnService (`service/NexusVpnService.kt`), 2. RuleEngine (`engine/RuleEngine.kt`), 3. DnsFilterEngine (`engine/DnsFilterEngine.kt`), 4. DnsUpstreamManager (`engine/DnsUpstreamManager.kt`), 5. PacketRouter (`engine/PacketRouter.kt`), 6. ConnectionTracker (`engine/ConnectionTracker.kt`), 7. ArgusProxyServer (`engine/proxy/ArgusProxyServer.kt`) (+3 more)

### Community 44 - "Spotify JS Transformer"
Cohesion: 0.20
Nodes (9): args, command, type, args, command, type, servers, fetch (+1 more)

### Community 45 - "YouTube JS Transformer"
Cohesion: 0.22
Nodes (8): Execution Plan, NexusBlock Overhaul Plan — Indian OTT + YouTube Adblocking, Phase 1: Full-Tunnel Packet Router (CRITICAL), Phase 2: Enhanced Ad Detection, Phase 3: YouTube IFrame Player, Phase 4: UI Overhaul with Dubai Font, Phase 5: Build & Deploy, Root Cause Analysis

### Community 46 - "App Build Config"
Cohesion: 0.22
Nodes (8): Acknowledgments, Architecture, code:block1 (┌───────────────────────────────────────────────────────────), Features, Future Improvements, License, NexusBlock — Device-Wide Ad Blocker for Android TV, Table of Contents

### Community 47 - "Project Settings"
Cohesion: 0.22
Nodes (9): AdGuard-Style Rule Engine, Advanced YouTube Ad Blocking, Custom Rules Editor, Diagnostics Screen, DNS-over-HTTPS Support, Notification Quick Actions, Per-App Connection Tracking, Recent Improvements (+1 more)

### Community 48 - "Root Build Config"
Cohesion: 0.29
Nodes (6): 1. Think Before Coding, 2. Simplicity First, 3. Surgical Changes, 4. Goal-Driven Execution, CLAUDE.md, code:block1 (1. [Step] → verify: [check])

### Community 49 - "Typography"
Cohesion: 0.29
Nodes (7): 1. Three-Tier Domain Matching + Trie, 2. SNI Inspection (Certificate Pinning Bypass), 3. NXDOMAIN Instead of Drops, 4. Batch Event Logging, 5. WorkManager with Constraints, 6. YouTube-Specific Rules, Optimizations & Hacks

### Community 50 - "Gradle Wrapper"
Cohesion: 0.33
Nodes (5): host_patterns, path_patterns, remove_fields, remove_objects_with_keys, set_null_fields

### Community 51 - "Android Plugin"
Cohesion: 0.33
Nodes (6): 1. Certificate Pinning, 2. Apps Using Private DNS (DoH/DoT), 3. IPv6-Only Networks, 4. Google Play Store Rejection, 5. YouTube Anti-Adblock Arms Race, Limitations & Workarounds

### Community 52 - "Kotlin Plugin"
Cohesion: 0.33
Nodes (6): Build Instructions, code:bash (./gradlew assembleDebug), code:bash (adb connect <TV_IP_ADDRESS>:5555), code:bash (adb logcat -s NexusBlock/DNS NexusBlock/Router NexusBlock/Pr), Prerequisites, Step-by-Step

### Community 54 - "KSP Plugin"
Cohesion: 0.50
Nodes (4): ADB Commands for Debugging, code:bash (# Real-time logs for all NexusBlock components), Manual Verification Checklist, Testing Guide

## Knowledge Gaps
- **155 isolated node(s):** `BeforeTool`, `DashboardUiState`, `Technique`, `BlockingTechniques`, `Protocol` (+150 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **34 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `BlockedDomain` connect `VPN DNS Update & Filtering` to `Network Packet Routing`, `QuickJS Transformer`?**
  _High betweenness centrality (0.012) - this node is a cross-community bridge._
- **Why does `RuleEngine` connect `VPN DNS Update & Filtering` to `Network Packet Routing`?**
  _High betweenness centrality (0.010) - this node is a cross-community bridge._
- **Are the 7 inferred relationships involving `RuleEngine` (e.g. with `.plainDomainBlocksSubdomains()` and `.exactAllowDoesNotAllowSiblingSubdomains()`) actually correct?**
  _`RuleEngine` has 7 INFERRED edges - model-reasoned connections that need verification._
- **Are the 11 inferred relationships involving `FocusPanel()` (e.g. with `CustomRulesScreen()` and `CustomRuleItem()`) actually correct?**
  _`FocusPanel()` has 11 INFERRED edges - model-reasoned connections that need verification._
- **What connects `BeforeTool`, `DashboardUiState`, `Technique` to the rest of the system?**
  _155 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `UI Screens & Compose Components` be split into smaller, more focused modules?**
  _Cohesion score 0.08979591836734693 - nodes in this community are weakly interconnected._
- **Should `VPN DNS Update & Filtering` be split into smaller, more focused modules?**
  _Cohesion score 0.05519480519480519 - nodes in this community are weakly interconnected._