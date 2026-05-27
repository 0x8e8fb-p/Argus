# Graph Report - NexusBlock-AndroidTV  (2026-05-23)

## Corpus Check
- 88 files · ~50,573 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 835 nodes · 1132 edges · 75 communities (35 shown, 40 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 94 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `c60289bd`
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
- [[_COMMUNITY_Community 63|Community 63]]
- [[_COMMUNITY_Community 64|Community 64]]
- [[_COMMUNITY_Community 65|Community 65]]
- [[_COMMUNITY_Community 66|Community 66]]
- [[_COMMUNITY_Community 67|Community 67]]
- [[_COMMUNITY_Community 68|Community 68]]
- [[_COMMUNITY_Community 69|Community 69]]
- [[_COMMUNITY_Community 70|Community 70]]
- [[_COMMUNITY_Community 71|Community 71]]
- [[_COMMUNITY_Community 72|Community 72]]
- [[_COMMUNITY_Community 73|Community 73]]
- [[_COMMUNITY_Community 74|Community 74]]

## God Nodes (most connected - your core abstractions)
1. `DnsFilterEngine` - 28 edges
2. `FocusPanel()` - 22 edges
3. `RuleEngine` - 22 edges
4. `TcpRelayEngine` - 21 edges
5. `NexusVpnService` - 15 edges
6. `ARGUS — Rethink & Redesign Document` - 15 edges
7. `BlockedDomainDao` - 14 edges
8. `PacketRouter` - 14 edges
9. `SettingsScreen()` - 13 edges
10. `DashboardScreen()` - 13 edges

## Surprising Connections (you probably didn't know these)
- `ArgusBlockApp()` --calls--> `dimensionsForWidth()`  [INFERRED]
  app/src/main/java/com/nexusblock/ui/MainActivity.kt → app/src/main/java/com/nexusblock/ui/theme/Dimensions.kt
- `ArgusBlockApp()` --calls--> `ArgusBackground()`  [INFERRED]
  app/src/main/java/com/nexusblock/ui/MainActivity.kt → app/src/main/java/com/nexusblock/ui/components/NexusTvComponents.kt
- `ArgusBlockApp()` --calls--> `ArgusVignette()`  [INFERRED]
  app/src/main/java/com/nexusblock/ui/MainActivity.kt → app/src/main/java/com/nexusblock/ui/components/Animations.kt
- `NexusBlockApp()` --calls--> `CustomRulesScreen()`  [INFERRED]
  app/src/main/java/com/nexusblock/ui/MainActivity.kt → app/src/main/java/com/nexusblock/ui/screens/CustomRulesScreen.kt
- `NexusBlockApp()` --calls--> `FirewallScreen()`  [INFERRED]
  app/src/main/java/com/nexusblock/ui/MainActivity.kt → app/src/main/java/com/nexusblock/ui/screens/FirewallScreen.kt

## Communities (75 total, 40 thin omitted)

### Community 0 - "UI Screens & Compose Components"
Cohesion: 0.06
Nodes (56): AnimatedCounter(), ArgusMotion, ArgusVignette(), AnimatedStateText(), ArgusBackground(), ArgusNavigationRail(), ArgusScreenHeader(), FocusPanel() (+48 more)

### Community 1 - "VPN DNS Update & Filtering"
Cohesion: 0.08
Nodes (8): CidrMatcher, DomainTrie, RuleEngine, TrieNode, RuleEngineTest, BlockedDomain, BuiltInBlockRules, RawBlocklistLoader

### Community 3 - "Blocklist Rule Engine"
Cohesion: 0.04
Nodes (45): 10. Key Engineering Decisions Documented, 11. Threat Model & Safety, 12. Legal / Distribution, 13. Next Step — Awaiting Your Approval, 1. Executive Summary: Why the Original Spec Was Wrong, 2. What the Stash/Surge Reddit Post Actually Reveals, 3. What NexusBlock Already Does Well (Preserve), 4.1 Remove: Go + Rust Native Components (+37 more)

### Community 4 - "Ad Skip Orchestrator & Accessibility"
Cohesion: 0.19
Nodes (5): bytesToIp(), calculateChecksum(), calculatePseudoChecksum(), PacketRouter, RouterStats

### Community 7 - "Configuration & Settings"
Cohesion: 0.16
Nodes (13): ArgusBlockTheme(), NexusBlockTheme(), Activity, Blocklists, CustomRules, Dashboard, Firewall, Home (+5 more)

### Community 9 - "Content Transformers"
Cohesion: 0.23
Nodes (3): start(), stop(), VpnWatchdogService

### Community 10 - "Domain DAO & Repository"
Cohesion: 0.13
Nodes (4): BlockingTechniques, SettingsRepository, VpnMode, VpnRoutingMode

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
Cohesion: 0.18
Nodes (13): Acknowledgments, Architecture, ArgusBlock — Ad Blocker for Android TV, Build, code:bash (# Debug build), Features, Future Improvements, License (+5 more)

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
Cohesion: 0.25
Nodes (8): Build Instructions, code:bash (adb connect <tv-ip>:5555), code:block3 (app/src/main/java/com/nexusblock/), code:bash (adb logcat -s NexusBlock/DNS NexusBlock/Router NexusBlock/Pr), Install on Android TV, Prerequisites, Project Structure, Step-by-Step

### Community 54 - "KSP Plugin"
Cohesion: 0.50
Nodes (4): ADB Commands for Debugging, code:bash (# Real-time logs for all NexusBlock components), Manual Verification Checklist, Testing Guide

### Community 63 - "Community 63"
Cohesion: 0.18
Nodes (3): SessionState, TcpRelayEngine, TcpSession

### Community 64 - "Community 64"
Cohesion: 0.16
Nodes (4): LunaState, LunaVpnManager, UdpRelayEngine, UdpSession

### Community 65 - "Community 65"
Cohesion: 0.13
Nodes (14): childDhGroup, childEncryption, childIntegrity, disableMobike, dns, enablePfs, ikeDhGroup, ikeEncryption (+6 more)

### Community 73 - "Community 73"
Cohesion: 0.83
Nodes (3): from(), IpFlowKey, ipv4ToInt()

## Knowledge Gaps
- **177 isolated node(s):** `BeforeTool`, `Home`, `Activity`, `DashboardUiState`, `AdvancedTab` (+172 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **40 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `RuleEngine` connect `VPN DNS Update & Filtering` to `UI Screens & Compose Components`, `Network Packet Routing`?**
  _High betweenness centrality (0.037) - this node is a cross-community bridge._
- **Are the 17 inferred relationships involving `FocusPanel()` (e.g. with `CustomRulesScreen()` and `CustomRuleItem()`) actually correct?**
  _`FocusPanel()` has 17 INFERRED edges - model-reasoned connections that need verification._
- **Are the 7 inferred relationships involving `RuleEngine` (e.g. with `.plainDomainBlocksSubdomains()` and `.exactAllowDoesNotAllowSiblingSubdomains()`) actually correct?**
  _`RuleEngine` has 7 INFERRED edges - model-reasoned connections that need verification._
- **What connects `BeforeTool`, `Home`, `Activity` to the rest of the system?**
  _177 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `UI Screens & Compose Components` be split into smaller, more focused modules?**
  _Cohesion score 0.060718252499074414 - nodes in this community are weakly interconnected._
- **Should `VPN DNS Update & Filtering` be split into smaller, more focused modules?**
  _Cohesion score 0.08456659619450317 - nodes in this community are weakly interconnected._
- **Should `Network Packet Routing` be split into smaller, more focused modules?**
  _Cohesion score 0.10483870967741936 - nodes in this community are weakly interconnected._