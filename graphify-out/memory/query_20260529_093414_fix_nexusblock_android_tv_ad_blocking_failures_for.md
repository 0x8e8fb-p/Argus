---
type: "query"
date: "2026-05-29T09:34:14.330554+00:00"
question: "Fix NexusBlock Android TV ad blocking failures for YouTube Hotstar Prime while keeping it lightweight"
contributor: "graphify"
source_nodes: ["SettingsRepository", "NexusVpnService", "PacketRouter", "DnsFilterEngine", "RuleEngine", "SniExtractor"]
---

# Q: Fix NexusBlock Android TV ad blocking failures for YouTube Hotstar Prime while keeping it lightweight

## Answer

Key failure found: default firewall modes ALLOWed major streaming packages, excluding them from the VPN. Fixed startup defaults to keep streaming apps filtered, default fresh installs to full-route mode for SNI/QUIC handling, keep custom rule observer alive across VPN restarts, sync DNS profile UI selection with DnsProfileManager, harden SNI parsing, curate default remote blocklists for Android TV memory, and add regression tests for Prime/Hotstar CDN patterns and SNI parsing. Verified testDebugUnitTest, assembleDebug, and assembleRelease. ADB had no connected device.

## Source Nodes

- SettingsRepository
- NexusVpnService
- PacketRouter
- DnsFilterEngine
- RuleEngine
- SniExtractor