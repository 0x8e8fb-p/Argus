# Graph Report - .  (2026-05-21)

## Corpus Check
- 97 files · ~57,383 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 768 nodes · 1107 edges · 66 communities (25 shown, 41 thin omitted)
- Extraction: 88% EXTRACTED · 12% INFERRED · 0% AMBIGUOUS · INFERRED: 130 edges (avg confidence: 0.82)
- Token cost: 0 input · 0 output

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
- [[_COMMUNITY_Gradle Wrapper|Gradle Wrapper]]
- [[_COMMUNITY_Android Plugin|Android Plugin]]
- [[_COMMUNITY_Kotlin Plugin|Kotlin Plugin]]
- [[_COMMUNITY_Hilt Plugin|Hilt Plugin]]
- [[_COMMUNITY_KSP Plugin|KSP Plugin]]
- [[_COMMUNITY_Compose Plugin|Compose Plugin]]
- [[_COMMUNITY_Project Settings Gradle|Project Settings Gradle]]
- [[_COMMUNITY_JitPack Repository|JitPack Repository]]
- [[_COMMUNITY_DNS Upstream Manager|DNS Upstream Manager]]
- [[_COMMUNITY_Enhanced Ad Detection Concept|Enhanced Ad Detection Concept]]
- [[_COMMUNITY_UI Design Overhaul|UI Design Overhaul]]
- [[_COMMUNITY_Guava Dependency|Guava Dependency]]
- [[_COMMUNITY_Ad Classification Labels|Ad Classification Labels]]
- [[_COMMUNITY_Dark Theme Colors|Dark Theme Colors]]
- [[_COMMUNITY_Light Theme Colors|Light Theme Colors]]
- [[_COMMUNITY_Dubai Font Family|Dubai Font Family]]

## God Nodes (most connected - your core abstractions)
1. `DnsFilterEngine` - 35 edges
2. `FocusPanel()` - 25 edges
3. `PacketRouter` - 24 edges
4. `RuleEngine` - 22 edges
5. `NexusVpnService` - 20 edges
6. `ArgusProxyServer` - 19 edges
7. `SettingsRepository` - 18 edges
8. `NexusBlockApp()` - 17 edges
9. `AdSkipOrchestrator` - 17 edges
10. `BlockedDomain` - 17 edges

## Surprising Connections (you probably didn't know these)
- `Rhino JavaScript Engine` --semantically_similar_to--> `Stash/Surge MITM+Script Approach`  [INFERRED] [semantically similar]
  app/build.gradle.kts → ARGUS_REDESIGN.md
- `TensorFlow Lite Dependency` --semantically_similar_to--> `LiteRTClassifier`  [INFERRED] [semantically similar]
  app/build.gradle.kts → ARGUS_REDESIGN.md
- `OkHttp Dependency` --references--> `ArgusProxyServer`  [INFERRED]
  app/build.gradle.kts → ARGUS_REDESIGN.md
- `Rhino JavaScript Engine` --references--> `TransformerEngine`  [INFERRED]
  app/build.gradle.kts → ARGUS_REDESIGN.md
- `NexusBlock App` --semantically_similar_to--> `NexusBlock Architecture`  [INFERRED] [semantically similar]
  README.md → CODEBASE_EXPLAINER.md

## Hyperedges (group relationships)
- **5-Layer Argus Architecture** — argus_redesign_layer_1_dns_sni, argus_redesign_layer_2_mitm_rewriter, argus_redesign_layer_3_accessibility_audio, argus_redesign_layer_4_strategy_router, argus_redesign_layer_5_ux [EXTRACTED 1.00]
- **MITM Proxy Pipeline** — argus_redesign_argus_proxy_server, argus_redesign_mitm_socket_handler, argus_redesign_transformer_engine [EXTRACTED 1.00]
- **Response Transformer Plugin Ecosystem** — argus_redesign_youtube_transformer, argus_redesign_spotify_transformer, argus_redesign_generic_json_transformer, transformers_youtube_transformer, transformers_spotify_transformer, transformers_hotstar_transformer [INFERRED 0.85]
- **Room Database Schema** — db_appdatabase_appdatabase, model_blockeddomain_blockeddomain, model_blockedevent_blockedevent, model_customrule_customrule [EXTRACTED 1.00]
- **Blocklist Processing Pipeline** — repository_blocklistsources_blocklistsources, repository_blocklistparsers_blocklistparsers, repository_rawblocklistloader_rawblocklistloader, repository_blocklistrepository_blocklistrepository [INFERRED 0.85]
- **Event Logging Stack** — model_blockedevent_blockedevent, db_blockedeventdao_blockedeventdao, repository_statsrepository_statsrepository [EXTRACTED 1.00]
- **VPN Packet Processing Pipeline** — engine_packetrouter_packetrouter, engine_tcpnatrelay_tcpnatrelay, engine_udprelay_udprelay, engine_dnsfilterengine_dnsfilterengine, engine_connectiontracker_connectiontracker [INFERRED 0.85]
- **DNS Filtering Stack** — engine_dnsfilterengine_dnsfilterengine, engine_ruleengine_ruleengine, engine_bloomfilter_bloomfilter, engine_dnsupstreammanager_dnsupstreammanager [INFERRED 0.85]
- **Socket Protection Layer** — engine_vpnprotector_vpnprotector, engine_vpnprotectedsocketfactory_vpnprotectedsocketfactory, engine_dnsupstreammanager_dnsupstreammanager [INFERRED 0.85]
- **MITM Proxy Pipeline** — proxy_argusproxyserver_argusproxyserver, proxy_mitmsockethandler_mitmsockethandler, transformers_transformerengine_transformerengine [EXTRACTED 1.00]
- **App Strategy System** — router_strategyrouter_strategyrouter, router_strategymatrix_strategymatrix, router_appprofile_appprofile [EXTRACTED 1.00]
- **VPN Lifecycle Orchestration** — service_nexusvpnservice_nexusvpnservice, service_vpnwatchdogservice_vpnwatchdogservice, service_bootreceiver_bootreceiver [EXTRACTED 1.00]
- **Navigation Components** — ui_mainactivity_nexusblockapp, components_nexustvcomponents_nexusnavigationrail, components_tvbottomnavigation_tvbottomnavigation [INFERRED 0.85]
- **FocusPanel Shared Component Usage** — screens_dashboard_dashboardscreen, screens_blocklist_blocklistscreen, screens_customrules_customrulesscreen, screens_firewall_firewallscreen, screens_settings_settingsscreen, screens_logs_logsscreen [INFERRED 0.85]
- **VPN Lifecycle Management** — ui_mainactivity_mainactivity, viewmodel_dashboard_dashboardviewmodel, viewmodel_firewall_firewallviewmodel [INFERRED 0.75]
- **SSAI detection pipeline** — vision_adskiporchestrator_adskiporchestrator, vision_audioaddetector_audioaddetector, vision_litertclassifier_litertclassifier [EXTRACTED 1.00]
- **Transformer plugin test suite** — transformers_quickjstransformertest_quickjstransformertest, plugins_youtubetransformertest_youtubetransformertest, plugins_spotifytransformertest_spotifytransformertest [INFERRED 0.85]
- **tun2socks native stack** — tun2socks_tun2socks_tun2socks, tun2socks_build_android_script, tun2socks_readme_readme [INFERRED 0.85]

## Communities (66 total, 41 thin omitted)

### Community 0 - "UI Screens & Compose Components"
Cohesion: 0.05
Nodes (61): AnimatedStateText(), FocusPanel(), MetricTile(), NavRailItem(), NexusBackground(), NexusNavigationRail(), NexusScreenHeader(), RailItem (+53 more)

### Community 1 - "VPN DNS Update & Filtering"
Cohesion: 0.05
Nodes (12): CustomRuleDao, BlocklistRepository, StatsRepository, DnsFilterEngine, DnsStats, isAlbaniaBlocked(), DnsMode, DnsUpstreamManager (+4 more)

### Community 2 - "Network Packet Routing"
Cohesion: 0.09
Nodes (12): bytesToIp(), calculateChecksum(), calculatePseudoChecksum(), NetworkUtils, PacketRouter, RouterStats, State, TcpNatRelay (+4 more)

### Community 3 - "Blocklist Rule Engine"
Cohesion: 0.08
Nodes (10): BloomFilter, CidrMatcher, DomainTrie, RuleEngine, TrieNode, RuleEngineTest, BlockedDomain, oisd small blocklist (+2 more)

### Community 4 - "Ad Skip Orchestrator & Accessibility"
Cohesion: 0.09
Nodes (4): ArgusAccessibilityService, AdSkipOrchestrator, FrameData, MediaProjectionCapture

### Community 5 - "VPN Service Lifecycle"
Cohesion: 0.09
Nodes (4): BootReceiver, NexusVpnService, UninstallCleanupReceiver, VpnWatchdogService

### Community 6 - "Architecture & Dependencies"
Cohesion: 0.07
Nodes (32): Compose TV Dependencies, kotlinx-serialization-protobuf, Rhino JavaScript Engine, TensorFlow Lite Dependency, 5-Layer Argus Architecture, AdSkipOrchestrator, AppProfile, ArgusAccessibilityService (+24 more)

### Community 7 - "Configuration & Settings"
Cohesion: 0.08
Nodes (8): FirewallMode, Constants, NexusBlockApplication, BlocklistFormat, BlocklistSourceDefinition, BlocklistSources, BlockingTechniques, SettingsRepository

### Community 8 - "Proxy Dependencies & Server"
Cohesion: 0.08
Nodes (31): BouncyCastle Dependency, dnsjava Dependency, Hilt Dependency, OkHttp Dependency, WorkManager Dependency, ArgusProxyServer, Decision: Kotlin-only Proxy, BloomFilter (+23 more)

### Community 9 - "Content Transformers"
Cohesion: 0.11
Nodes (6): GenericJsonTransformer, TransformRule, SpotifyTransformer, YoutubeTransformer, TransformerEngine, TransformerPlugin

### Community 10 - "Domain DAO & Repository"
Cohesion: 0.09
Nodes (3): BlockedDomainDao, BlocklistSourceState, BlocklistRepository

### Community 12 - "Certificate Management"
Cohesion: 0.13
Nodes (3): CaCertificateManager, SettingsRepository, CertificateManager

### Community 13 - "MITM SSL Handling"
Cohesion: 0.24
Nodes (3): HttpRequest, MitmSocketHandler, ParsedResponse

### Community 19 - "QuickJS Transformer"
Cohesion: 0.27
Nodes (4): JsPlugin, parseMetadata(), QuickJsTransformer, toWildcardRegex()

### Community 22 - "tun2socks Native Stack"
Cohesion: 0.31
Nodes (7): tun2socks README, checkSocks5(), main(), openTUN(), setLogLevel(), StartTun2SocksJNI(), tun2socks main

### Community 26 - "Audio Ad Detection"
Cohesion: 0.33
Nodes (5): AudioAdDetector, AudioAdState, ConfirmedAd, Content, PossibleAd

### Community 27 - "LiteRT Ad Classifier"
Cohesion: 0.42
Nodes (3): ClassificationResult, LiteRTClassifier, unknown()

### Community 33 - "Hotstar Transformer Rules"
Cohesion: 0.33
Nodes (5): host_patterns, path_patterns, remove_fields, remove_objects_with_keys, set_null_fields

### Community 42 - "DataStore & Room"
Cohesion: 0.67
Nodes (3): DataStore Preferences, Room Dependency, AppDatabase

## Knowledge Gaps
- **67 isolated node(s):** `Dashboard`, `Blocklists`, `CustomRules`, `Firewall`, `Logs` (+62 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **41 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `NexusVpnService` connect `VPN Service Lifecycle` to `VPN DNS Update & Filtering`, `Native TUN Bridge`, `MITM SSL Handling`?**
  _High betweenness centrality (0.338) - this node is a cross-community bridge._
- **Why does `BlocklistUpdateWorker` connect `VPN DNS Update & Filtering` to `VPN Service Lifecycle`, `Configuration & Settings`?**
  _High betweenness centrality (0.335) - this node is a cross-community bridge._
- **Why does `ArgusProxyServer` connect `Native TUN Bridge` to `Strategy Router`, `Content Transformers`, `VPN Service Lifecycle`, `MITM SSL Handling`?**
  _High betweenness centrality (0.313) - this node is a cross-community bridge._
- **Are the 12 inferred relationships involving `FocusPanel()` (e.g. with `CustomRulesScreen()` and `CustomRuleItem()`) actually correct?**
  _`FocusPanel()` has 12 INFERRED edges - model-reasoned connections that need verification._
- **Are the 2 inferred relationships involving `PacketRouter` (e.g. with `QuicInspector` and `ConnectionTracker`) actually correct?**
  _`PacketRouter` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Are the 8 inferred relationships involving `RuleEngine` (e.g. with `.plainDomainBlocksSubdomains()` and `.exactAllowDoesNotAllowSiblingSubdomains()`) actually correct?**
  _`RuleEngine` has 8 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Dashboard`, `Blocklists`, `CustomRules` to the rest of the system?**
  _73 weakly-connected nodes found - possible documentation gaps or missing edges._