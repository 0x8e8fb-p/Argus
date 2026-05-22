# Graph Report - .  (2026-05-22)

## Corpus Check
- 60 files · ~35,000 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 382 nodes · 503 edges · 40 communities (8 shown, 32 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 55 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_Community 2|Community 2]]
- [[_COMMUNITY_Community 3|Community 3]]
- [[_COMMUNITY_Community 4|Community 4]]
- [[_COMMUNITY_Community 5|Community 5]]
- [[_COMMUNITY_Community 6|Community 6]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Community 15|Community 15]]
- [[_COMMUNITY_Community 16|Community 16]]
- [[_COMMUNITY_Community 17|Community 17]]
- [[_COMMUNITY_Community 18|Community 18]]
- [[_COMMUNITY_Community 19|Community 19]]
- [[_COMMUNITY_Community 20|Community 20]]
- [[_COMMUNITY_Community 21|Community 21]]
- [[_COMMUNITY_Community 22|Community 22]]
- [[_COMMUNITY_Community 23|Community 23]]
- [[_COMMUNITY_Community 24|Community 24]]
- [[_COMMUNITY_Community 25|Community 25]]
- [[_COMMUNITY_Community 26|Community 26]]
- [[_COMMUNITY_Community 27|Community 27]]
- [[_COMMUNITY_Community 28|Community 28]]
- [[_COMMUNITY_Community 29|Community 29]]
- [[_COMMUNITY_Community 30|Community 30]]
- [[_COMMUNITY_Community 31|Community 31]]
- [[_COMMUNITY_Community 32|Community 32]]
- [[_COMMUNITY_Community 33|Community 33]]
- [[_COMMUNITY_Community 34|Community 34]]
- [[_COMMUNITY_Community 35|Community 35]]
- [[_COMMUNITY_Community 36|Community 36]]
- [[_COMMUNITY_Community 38|Community 38]]

## God Nodes (most connected - your core abstractions)
1. `DnsFilterEngine` - 20 edges
2. `RuleEngine` - 18 edges
3. `FocusPanel()` - 15 edges
4. `NexusVpnService` - 14 edges
5. `BlockedDomainDao` - 13 edges
6. `PacketRouter` - 13 edges
7. `ConnectionTracker` - 12 edges
8. `BlockedDomain` - 11 edges
9. `NexusBlockApp()` - 10 edges
10. `BlocklistRepository` - 10 edges

## Surprising Connections (you probably didn't know these)
- `ProtectionPanel()` --calls--> `StatusDot()`  [INFERRED]
  app/src/main/java/com/nexusblock/ui/screens/DashboardScreen.kt → main/java/com/nexusblock/ui/components/NexusTvComponents.kt
- `ProtectionPanel()` --calls--> `AnimatedStateText()`  [INFERRED]
  app/src/main/java/com/nexusblock/ui/screens/DashboardScreen.kt → main/java/com/nexusblock/ui/components/NexusTvComponents.kt
- `NexusBlockApp()` --calls--> `DashboardScreen()`  [INFERRED]
  main/java/com/nexusblock/ui/MainActivity.kt → app/src/main/java/com/nexusblock/ui/screens/DashboardScreen.kt
- `NexusBlockApp()` --calls--> `SettingsScreen()`  [INFERRED]
  main/java/com/nexusblock/ui/MainActivity.kt → app/src/main/java/com/nexusblock/ui/screens/SettingsScreen.kt
- `SettingsScreen()` --calls--> `NexusScreenHeader()`  [INFERRED]
  app/src/main/java/com/nexusblock/ui/screens/SettingsScreen.kt → main/java/com/nexusblock/ui/components/NexusTvComponents.kt

## Communities (40 total, 32 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.12
Nodes (29): AnimatedStateText(), FocusPanel(), MetricTile(), NavRailItem(), NexusBackground(), NexusNavigationRail(), NexusScreenHeader(), RailItem (+21 more)

### Community 1 - "Community 1"
Cohesion: 0.08
Nodes (5): RuleEngineTest, BlockedDomain, BlocklistRepository, BuiltInBlockRules, RawBlocklistLoader

### Community 3 - "Community 3"
Cohesion: 0.19
Nodes (4): CidrMatcher, DomainTrie, RuleEngine, TrieNode

### Community 4 - "Community 4"
Cohesion: 0.20
Nodes (4): calculateChecksum(), calculatePseudoChecksum(), PacketRouter, RouterStats

### Community 7 - "Community 7"
Cohesion: 0.16
Nodes (9): NexusBlockTheme(), Blocklists, CustomRules, Dashboard, Firewall, Logs, MainActivity, Screen (+1 more)

### Community 31 - "Community 31"
Cohesion: 0.50
Nodes (3): BlocklistFormat, BlocklistSourceDefinition, BlocklistSources

## Knowledge Gaps
- **20 isolated node(s):** `Constants`, `Screen`, `Dashboard`, `Blocklists`, `CustomRules` (+15 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **32 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `BlockedDomain` connect `Community 1` to `Community 2`, `Community 19`?**
  _High betweenness centrality (0.028) - this node is a cross-community bridge._
- **Why does `RuleEngine` connect `Community 3` to `Community 1`, `Community 2`?**
  _High betweenness centrality (0.023) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `RuleEngine` (e.g. with `.plainDomainBlocksSubdomains()` and `.exactAllowDoesNotAllowSiblingSubdomains()`) actually correct?**
  _`RuleEngine` has 6 INFERRED edges - model-reasoned connections that need verification._
- **Are the 11 inferred relationships involving `FocusPanel()` (e.g. with `CustomRulesScreen()` and `CustomRuleItem()`) actually correct?**
  _`FocusPanel()` has 11 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Constants`, `Screen`, `Dashboard` to the rest of the system?**
  _20 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.11904761904761904 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.07936507936507936 - nodes in this community are weakly interconnected._