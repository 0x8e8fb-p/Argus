# Graph Report - NexusBlock-AndroidTV  (2026-05-29)

## Corpus Check
- 84 files · ~47,154 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 913 nodes · 1403 edges · 80 communities (48 shown, 32 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 105 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `10cbca64`
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
- [[_COMMUNITY_App Database|App Database]]
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
- [[_COMMUNITY_Project Settings Gradle|Project Settings Gradle]]
- [[_COMMUNITY_JitPack Repository|JitPack Repository]]
- [[_COMMUNITY_DNS Upstream Manager|DNS Upstream Manager]]
- [[_COMMUNITY_Guava Dependency|Guava Dependency]]
- [[_COMMUNITY_Ad Classification Labels|Ad Classification Labels]]
- [[_COMMUNITY_Community 63|Community 63]]
- [[_COMMUNITY_Community 64|Community 64]]
- [[_COMMUNITY_Community 65|Community 65]]
- [[_COMMUNITY_Community 69|Community 69]]
- [[_COMMUNITY_Community 73|Community 73]]
- [[_COMMUNITY_Community 74|Community 74]]
- [[_COMMUNITY_Community 75|Community 75]]
- [[_COMMUNITY_Community 76|Community 76]]
- [[_COMMUNITY_Community 77|Community 77]]
- [[_COMMUNITY_Community 78|Community 78]]
- [[_COMMUNITY_Community 79|Community 79]]

## God Nodes (most connected - your core abstractions)
1. `DnsFilterEngine` - 32 edges
2. `RuleEngine` - 26 edges
3. `BlockedDomain` - 25 edges
4. `TcpRelayEngine` - 23 edges
5. `FocusPanel()` - 22 edges
6. `ARGUS — Rethink & Redesign Document` - 18 edges
7. `MainActivity` - 17 edges
8. `DashboardScreen()` - 17 edges
9. `PacketRouter` - 17 edges
10. `AdvancedSettingsScreen()` - 16 edges

## Surprising Connections (you probably didn't know these)
- `MetricTile()` --calls--> `AnimatedCounter()`  [INFERRED]
  app/src/main/java/com/nexusblock/ui/components/NexusTvComponents.kt → app/src/main/java/com/nexusblock/ui/components/Animations.kt
- `CustomRuleItem()` --calls--> `FocusPanel()`  [INFERRED]
  app/src/main/java/com/nexusblock/ui/screens/CustomRulesScreen.kt → app/src/main/java/com/nexusblock/ui/components/NexusTvComponents.kt
- `BlocklistItem()` --calls--> `FocusPanel()`  [INFERRED]
  app/src/main/java/com/nexusblock/ui/screens/BlocklistScreen.kt → app/src/main/java/com/nexusblock/ui/components/NexusTvComponents.kt
- `AddRuleDialog()` --calls--> `SegmentedControl()`  [INFERRED]
  app/src/main/java/com/nexusblock/ui/screens/CustomRulesScreen.kt → app/src/main/java/com/nexusblock/ui/components/NexusTvComponents.kt
- `ArgusBlockApp()` --calls--> `ArgusVignette()`  [INFERRED]
  app/src/main/java/com/nexusblock/ui/MainActivity.kt → app/src/main/java/com/nexusblock/ui/components/Animations.kt

## Communities (80 total, 32 thin omitted)

### Community 0 - "UI Screens & Compose Components"
Cohesion: 0.06
Nodes (59): AnimatedCounter(), ArgusMotion, ArgusVignette(), AnimatedStateText(), ArgusBackground(), ArgusNavigationRail(), ArgusScreenHeader(), FocusPanel() (+51 more)

### Community 1 - "VPN DNS Update & Filtering"
Cohesion: 0.05
Nodes (50): 10. Guidelines for AI Code Modifications, 1. Core Architecture & System Overview, 2. Directory Map, 3. Key Network & Process Flows, 3. Navigation & Screens, 4. Architecture Layers, 5. Feature Inventory (Live), 5. Guidelines for AI Code Modifications (+42 more)

### Community 2 - "Network Packet Routing"
Cohesion: 0.13
Nodes (14): 11. Threat Model & Safety, 12. Legal / Distribution, 13. Next Step — Awaiting Your Approval, 1. Executive Summary: Why the Original Spec Was Wrong, 2. What the Stash/Surge Reddit Post Actually Reveals, 3. What Argus Already Does Well (Preserve), 3. What NexusBlock Already Does Well (Preserve), 5. The New 5-Layer Architecture (Instead of 7) (+6 more)

### Community 3 - "Blocklist Rule Engine"
Cohesion: 0.07
Nodes (9): BlockedDomainDao, BlocklistSourceState, BlockedDomain, BlocklistRepository, BuiltInBlockRules, RawBlocklistLoader, BlocklistUpdateWorker, runNow() (+1 more)

### Community 4 - "Ad Skip Orchestrator & Accessibility"
Cohesion: 0.13
Nodes (3): DnsFilterEngine, DnsStats, isUpstreamDomain()

### Community 5 - "VPN Service Lifecycle"
Cohesion: 0.19
Nodes (3): SessionState, TcpRelayEngine, TcpSession

### Community 6 - "Architecture & Dependencies"
Cohesion: 0.06
Nodes (5): AppDatabase, BlockedEventDao, CustomRuleDao, BlockedEvent, StatsRepository

### Community 7 - "Configuration & Settings"
Cohesion: 0.14
Nodes (5): CidrMatcher, DomainTrie, RuleEngine, TrieNode, RuleEngineTest

### Community 9 - "Content Transformers"
Cohesion: 0.10
Nodes (14): CustomRule, ArgusBlockTheme(), ArgusTheme(), NexusBlockTheme(), Activity, Blocklists, Dashboard, Firewall (+6 more)

### Community 10 - "Domain DAO & Repository"
Cohesion: 0.23
Nodes (5): bytesToIp(), calculateChecksum(), calculatePseudoChecksum(), PacketRouter, RouterStats

### Community 11 - "Native TUN Bridge"
Cohesion: 0.14
Nodes (9): ConnectionTracker, 2. Module-by-Module Technical Breakdown, 2. `VpnWatchdogService.kt` & `BootReceiver.kt`, 3. `RuleEngine.kt` & `BloomFilter.kt`, 5. `MitmProxyManager.kt` & `CertificateManager.kt`, Android Core Services (`service/`), Core Constants: `com.nexusblock.Constants`, Dependency Injection Modules (`di/`) (+1 more)

### Community 12 - "Certificate Management"
Cohesion: 0.10
Nodes (19): 10. Subscription / Monetization, 1. Architecture Overview, 2. Native VPN (strongSwan), 3. YouTube Strategy (CRITICAL INSIGHT), 4. Blocklist Infrastructure, 5. DNS/Filtering Approach, 6. Certificate Management, 7. In-App Browser (+11 more)

### Community 13 - "MITM SSL Handling"
Cohesion: 0.18
Nodes (4): LunaState, LunaVpnManager, UdpRelayEngine, UdpSession

### Community 14 - "Transformer Tests"
Cohesion: 0.10
Nodes (5): BlockingTechniques, SettingsRepository, VpnMode, VpnRoutingMode, VpnRoutingModeTest

### Community 15 - "Connection Tracker"
Cohesion: 0.14
Nodes (18): Argus Ad Blocker — DNS + SNI + MITM API Rewriter MVP, Build Verification ✅, code:bash (./gradlew clean assembleDebug), Defect Triage Categories, Regression Tests, Required Accounts, Required Hardware, Sign-Off Checklist (+10 more)

### Community 16 - "QUIC Inspector Tests"
Cohesion: 0.13
Nodes (14): childDhGroup, childEncryption, childIntegrity, disableMobike, dns, enablePfs, ikeDhGroup, ikeEncryption (+6 more)

### Community 19 - "QuickJS Transformer"
Cohesion: 0.33
Nodes (10): Argus — Ad Blocker for Android TV, ArgusBlock — Ad Blocker for Android TV, Build, Features, Install on Android TV, License, Project Structure, Supported DNS Providers (+2 more)

### Community 22 - "tun2socks Native Stack"
Cohesion: 0.18
Nodes (11): 10. Background Resilience, 1. NexusVpnService (`service/NexusVpnService.kt`), 2. RuleEngine (`engine/RuleEngine.kt`), 3. DnsFilterEngine (`engine/DnsFilterEngine.kt`), 4. DnsUpstreamManager (`engine/DnsUpstreamManager.kt`), 5. PacketRouter (`engine/PacketRouter.kt`), 6. ConnectionTracker (`engine/ConnectionTracker.kt`), 7. ArgusProxyServer (`engine/proxy/ArgusProxyServer.kt`) (+3 more)

### Community 23 - "Dashboard ViewModel"
Cohesion: 0.26
Nodes (3): start(), stop(), VpnWatchdogService

### Community 26 - "Audio Ad Detection"
Cohesion: 0.22
Nodes (9): AdGuard-Style Rule Engine, Advanced YouTube Ad Blocking, Custom Rules Editor, Diagnostics Screen, DNS-over-HTTPS Support, Notification Quick Actions, Per-App Connection Tracking, Recent Improvements (+1 more)

### Community 27 - "LiteRT Ad Classifier"
Cohesion: 0.22
Nodes (8): Execution Plan, NexusBlock Overhaul Plan — Indian OTT + YouTube Adblocking, Phase 1: Full-Tunnel Packet Router (CRITICAL), Phase 2: Enhanced Ad Detection, Phase 3: YouTube IFrame Player, Phase 4: UI Overhaul with Dubai Font, Phase 5: Build & Deploy, Root Cause Analysis

### Community 30 - "Custom Rules UI"
Cohesion: 0.33
Nodes (6): Build Instructions, code:bash (adb connect <tv-ip>:5555), code:block3 (app/src/main/java/com/nexusblock/), code:bash (adb logcat -s NexusBlock/DNS NexusBlock/Router NexusBlock/Pr), Prerequisites, Step-by-Step

### Community 32 - "Strategy Router"
Cohesion: 0.29
Nodes (6): 1. Think Before Coding, 2. Simplicity First, 3. Surgical Changes, 4. Goal-Driven Execution, CLAUDE.md, code:block1 (1. [Step] → verify: [check])

### Community 33 - "Hotstar Transformer Rules"
Cohesion: 0.29
Nodes (7): 1. Three-Tier Domain Matching + Trie, 2. SNI Inspection (Certificate Pinning Bypass), 3. NXDOMAIN Instead of Drops, 4. Batch Event Logging, 5. WorkManager with Constraints, 6. YouTube-Specific Rules, Optimizations & Hacks

### Community 35 - "QUIC Inspector"
Cohesion: 0.29
Nodes (6): BlocklistFormat, BlocklistSourceDefinition, BlocklistSources, BlocklistSource, BlocklistUpdateResult, BlocklistViewModel

### Community 36 - "Strategy Matrix"
Cohesion: 0.43
Nodes (6): args, command, type, servers, fetch, filesystem

### Community 37 - "Firewall ViewModel"
Cohesion: 0.33
Nodes (5): host_patterns, path_patterns, remove_fields, remove_objects_with_keys, set_null_fields

### Community 38 - "App DI Module"
Cohesion: 0.33
Nodes (5): host_patterns, path_patterns, remove_fields, remove_objects_with_keys, set_null_fields

### Community 39 - "App Database"
Cohesion: 0.33
Nodes (6): 1. Certificate Pinning, 2. Apps Using Private DNS (DoH/DoT), 3. IPv6-Only Networks, 4. Google Play Store Rejection, 5. YouTube Anti-Adblock Arms Race, Limitations & Workarounds

### Community 50 - "Gradle Wrapper"
Cohesion: 0.20
Nodes (10): Acknowledgments, ADB Commands for Debugging, Architecture, code:bash (# Debug build), code:bash (# Real-time logs for all NexusBlock components), Future Improvements, Manual Verification Checklist, NexusBlock — Device-Wide Ad Blocker for Android TV (+2 more)

### Community 53 - "Hilt Plugin"
Cohesion: 0.47
Nodes (3): cancel(), enqueue(), VpnWatchdogWorker

### Community 54 - "KSP Plugin"
Cohesion: 0.83
Nodes (3): byId(), Category, DnsProviderProfile

### Community 55 - "Compose Plugin"
Cohesion: 1.00
Nodes (3): from(), IpFlowKey, ipv4ToInt()

### Community 56 - "Project Settings Gradle"
Cohesion: 0.19
Nodes (4): SniExtractorTest, FlowCache, removeEldestEntry(), Verdict

### Community 74 - "Community 74"
Cohesion: 0.28
Nodes (9): 9. File-by-File Migration from Argus → Argus, 9. File-by-File Migration from NexusBlock → Argus, Build Dependencies Add/Remove, code:block3 (com.nexusblock → com.argus.adblock), code:block4 (app/src/main/java/com/argus/adblock/), code:kotlin (// LittleProxy + Netty — too heavy, replaced by custom proxy), code:kotlin (// LiteRT (TensorFlow Lite rebranded) for on-device inferenc), New Directory Structure (+1 more)

### Community 75 - "Community 75"
Cohesion: 0.25
Nodes (8): 7. Layer-by-Layer Design, code:block2 (App HTTPS Request → VpnService TUN → PacketRouter (passes 44), LAYER 1 — DNS + SNI (Refined Argus), LAYER 1 — DNS + SNI (Refined NexusBlock), LAYER 2 — MITM API Response Rewriter (The Stash Layer), LAYER 3 — Accessibility + Audio "Eyes" (SSAI Killer), LAYER 4 — Per-App Strategy Router, LAYER 5 — UX (Refined Compose TV)

### Community 76 - "Community 76"
Cohesion: 0.29
Nodes (7): 4.1 Remove: Go + Rust Native Components, 4.2 Remove: LSPosed / Xposed / Frida as Core Architecture, 4.3 Remove: VirtualXposed / BlackBox Sandbox, 4.4 Remove: Full on-device TFLite training pipeline, 4.5 Remove: LittleProxy + Netty, 4.6 Remove: Full HLS/DASH manifest rewriter as Sprint 1/2 goal, 4. What Must Change (Hard Rethinks)

### Community 77 - "Community 77"
Cohesion: 0.33
Nodes (6): 8. Realistic Sprint Roadmap (Buildable), Sprint 1: Layer 1 + Layer 2 MVP (Weeks 1–3), Sprint 2: Layer 3 — Accessibility + Audio (Weeks 4–5), Sprint 3: Layer 4 Router + Expansion (Weeks 6–7), Sprint 4: Rooted Power-User Features (Weeks 8–9), Sprint 5: Novel Features (Weeks 10–12)

### Community 78 - "Community 78"
Cohesion: 0.40
Nodes (5): 10. Key Engineering Decisions Documented, Decision: AccessibilityService for SSAI, not manifest rewriting, Decision: Kotlin-only proxy instead of LittleProxy/Go/Rust, Decision: No on-device training; federated model download instead, Decision: User-installed CA for no-root instead of bypass tricks

### Community 79 - "Community 79"
Cohesion: 0.50
Nodes (3): Answer, Q: Fix NexusBlock Android TV ad blocking failures for YouTube Hotstar Prime while keeping it lightweight, Source Nodes

## Knowledge Gaps
- **165 isolated node(s):** `Constants`, `BlocklistSources`, `host_patterns`, `path_patterns`, `remove_fields` (+160 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **32 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Low-Level Packet Engine (`engine/`)` connect `Native TUN Bridge` to `Domain DAO & Repository`, `Ad Skip Orchestrator & Accessibility`?**
  _High betweenness centrality (0.093) - this node is a cross-community bridge._
- **Why does `RuleEngine` connect `Configuration & Settings` to `UI Screens & Compose Components`, `Ad Skip Orchestrator & Accessibility`?**
  _High betweenness centrality (0.091) - this node is a cross-community bridge._
- **Why does `BlockedDomain` connect `Blocklist Rule Engine` to `Ad Skip Orchestrator & Accessibility`, `Architecture & Dependencies`, `Configuration & Settings`?**
  _High betweenness centrality (0.053) - this node is a cross-community bridge._
- **Are the 9 inferred relationships involving `RuleEngine` (e.g. with `.plainDomainBlocksSubdomains()` and `.exactAllowDoesNotAllowSiblingSubdomains()`) actually correct?**
  _`RuleEngine` has 9 INFERRED edges - model-reasoned connections that need verification._
- **Are the 10 inferred relationships involving `BlockedDomain` (e.g. with `.plainDomainBlocksSubdomains()` and `.exactAllowDoesNotAllowSiblingSubdomains()`) actually correct?**
  _`BlockedDomain` has 10 INFERRED edges - model-reasoned connections that need verification._
- **Are the 17 inferred relationships involving `FocusPanel()` (e.g. with `SettingsScreen()` and `SettingSwitchRow()`) actually correct?**
  _`FocusPanel()` has 17 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Constants`, `BlocklistSources`, `host_patterns` to the rest of the system?**
  _165 weakly-connected nodes found - possible documentation gaps or missing edges._