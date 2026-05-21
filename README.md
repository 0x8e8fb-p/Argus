# NexusBlock — Device-Wide Ad Blocker for Android TV

**NexusBlock** is a root-free, system-wide ad blocker for Android TV that replicates and extends the core techniques used by Luna AdBlocker and RethinkDNS. It leverages Android's `VpnService` API to intercept all network traffic, filters DNS queries against massive blocklists, inspects TLS SNI headers to block HTTPS ad domains, runs an MITM proxy for deep HTTPS inspection, and supports per-app connection tracking.

---

## Table of Contents

1. [Features](#features)
2. [Architecture](#architecture)
3. [Key Components](#key-components)
4. [Build Instructions](#build-instructions)
5. [Testing Guide](#testing-guide)
6. [Recent Improvements](#recent-improvements)
7. [Optimizations & Hacks](#optimizations--hacks)
8. [Limitations & Workarounds](#limitations--workarounds)
9. [Future Improvements](#future-improvements)

---

## Features

| Feature | Status | Notes |
|---------|--------|-------|
| VPN-based traffic interception | ✅ Complete | `VpnService` with TUN interface |
| DNS-level ad blocking | ✅ Complete | Custom UDP relay with dnsjava parsing |
| Multi-source blocklists | ✅ Complete | Firebog, OISD, AdGuard, StevenBlack |
| Auto blocklist updates | ✅ Complete | WorkManager daily worker |
| TLS SNI inspection | ✅ Complete | Parses ClientHello without decryption |
| MITM HTTPS proxy | ✅ Complete | Kotlin ArgusProxyServer + BouncyCastle CA |
| YouTube ad blocking | ✅ Complete | DNS + SNI + regex path rules |
| App whitelist | ✅ Complete | Per-app VPN bypass |
| Auto-start on boot | ✅ Complete | `BOOT_COMPLETED` receiver |
| Auto-reconnect | ✅ Complete | Network callback + watchdog alarm |
| Battery optimization bypass | ✅ Complete | Guides user to whitelist |
| Blocked ads logging | ✅ Complete | Room database with batch inserts |
| TV-optimized UI | ✅ Complete | Jetpack Compose for TV |
| Real-time stats | ✅ Complete | Ads blocked, data saved, active rules |
| AdGuard-style rule parser | ✅ Complete | `||domain^`, `@@||domain^`, `/regex/` |
| Per-app connection tracking | ✅ Complete | procfs + ConnectivityService |
| Custom rules editor | ✅ Complete | Add block/allow rules from UI |
| DoH/DoT upstream DNS | ✅ Complete | Cloudflare, Google, Quad9 |
| Diagnostics screen | ✅ Complete | Real-time packet/DNS stats |
| Notification quick actions | ✅ Complete | Stop/Restart from notification |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         NEXUSBLOCK APP                               │
├─────────────────────────────────────────────────────────────────────┤
│  TV UI Layer (Jetpack Compose for TV)                               │
│  ├── Dashboard (toggle, stats, connection status)                   │
│  ├── Blocklist Manager (enable/disable sources)                     │
│  ├── Custom Rules Editor (add block/allow rules)                    │
│  ├── App Whitelist (bypass VPN per app)                             │
│  ├── Blocked Ads Log (searchable/filterable)                        │
│  ├── Diagnostics (real-time packet/DNS stats)                       │
│  └── Settings (auto-start, DNS mode, battery, CA cert)              │
├─────────────────────────────────────────────────────────────────────┤
│  Core Engine Layer                                                   │
│  ├── NexusVpnService (VpnService subclass)                          │
│  │   ├── Creates TUN interface (routes all traffic)                 │
│  │   ├── Intercepts DNS → forwards to local DNS proxy               │
│  │   ├── Intercepts TCP → SNI inspection + MITM proxy              │
│  │   └── Applies allowed/disallowed apps (whitelist)                │
│  ├── DnsFilterEngine (local DNS proxy)                              │
│  │   ├── Integrated RuleEngine (AdGuard/Adblock syntax)             │
│  │   ├── DnsUpstreamManager (Plain/DoH/DoT)                        │
│  │   ├── Matches queries against ad domains                         │
│  │   └── Returns NXDOMAIN for blocked domains                       │
│  ├── ArgusProxyServer (HTTPS inspection)                            │
│  │   ├── Local HTTP/HTTPS proxy (Kotlin coroutine-based)            │
│  │   ├── Filters requests by URL/Host against blocklists            │
│  │   ├── Generates on-device CA certificate                         │
│  │   └── Handles YouTube ad API calls (√)                           │
│  ├── PacketRouter                                                   │
│  │   └── Reads TUN fd, parses IP/UDP/TCP, routes to engines         │
│  └── ConnectionTracker                                              │
│      └── Maps connections to apps (procfs / modern API)             │
├─────────────────────────────────────────────────────────────────────┤
│  Data & Scheduling Layer                                             │
│  ├── BlocklistRepository (Room)                                     │
│  ├── CustomRuleRepository (user-defined rules)                      │
│  ├── BlocklistUpdateWorker (WorkManager)                            │
│  └── StatsRepository (blocked events)                               │
├─────────────────────────────────────────────────────────────────────┤
│  Background Resilience Layer                                         │
│  ├── BootReceiver (BOOT_COMPLETED)                                  │
│  ├── NetworkMonitor (ConnectivityManager)                           │
│  ├── VpnWatchdogService (30s health check + AlarmManager)           │
│  └── BatteryOptimizationHelper                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Key Components

### 1. NexusVpnService (`service/NexusVpnService.kt`)
The heart of the app. Extends Android's `VpnService` to create a virtual network interface (TUN). All device traffic is routed through this interface.

- **TUN configuration**: Sets up `10.0.0.2/24` with custom DNS at `10.0.0.1`
- **Foreground service**: Persistent notification with quick actions (Stop, Restart)
- **START_STICKY**: Automatically restarted by the OS if terminated
- **Network callback**: Re-establishes TUN when WiFi/Ethernet changes
- **Whitelist support**: Uses `addDisallowedApplication()` to let selected apps bypass the VPN
- **Notification actions**: Tap Stop or Restart directly from the notification shade

### 2. RuleEngine (`engine/RuleEngine.kt`)
A full-featured AdGuard/Adblock-style rule parser and matcher.

Supports syntax:
- `||example.org^` — block domain and all subdomains
- `@@||example.org^` — allow (exception) domain and subdomains
- `|example.org` — prefix match
- `example.org|` — suffix match
- `/regex/` — regular expression matching
- `127.0.0.1 example.org` — hosts file format
- `example.org` — plain domain

Matching algorithm:
- **HashSet** for exact domain matches (O(1))
- **Trie** for subdomain wildcard matching
- **Prefix/suffix lists** for anchored patterns
- **Regex list** for dynamic patterns

Exception rules (`@@`) are checked first and override block rules.

### 3. DnsFilterEngine (`engine/DnsFilterEngine.kt`)
Lightweight UDP DNS proxy with advanced rule matching.

- **Message parsing**: Uses `dnsjava` to parse DNS packets
- **RuleEngine integration**: Uses the same AdGuard-style rules as other layers
- **NXDOMAIN responses**: Blocked queries get immediate NXDOMAIN (no timeouts)
- **DnsUpstreamManager**: Supports Plain DNS, Cloudflare DoH, Google DoH, Quad9 DoH
- **Custom rules**: Combines blocklist domains with user-defined custom rules
- **Batch logging**: Blocked events queued in memory, flushed to Room every 5s

### 4. DnsUpstreamManager (`engine/DnsUpstreamManager.kt`)
Manages upstream DNS resolution with multiple transport options.

- **Plain DNS**: UDP to 8.8.8.8, 1.1.1.1, 9.9.9.9 with failover
- **DNS-over-HTTPS (DoH)**: Cloudflare, Google, Quad9 via OkHttp
- **Failover**: If DoH fails, automatically falls back to plain DNS
- **Switchable at runtime**: Change upstream without restarting VPN

### 5. PacketRouter (`engine/PacketRouter.kt`)
Reads raw IP packets from the TUN `FileChannel` and routes them.

- **IPv4 parsing**: Extracts headers, protocols, ports
- **IPv6 passthrough**: Routes IPv6 traffic (full parsing can be added)
- **SNI inspection**: Parses TLS ClientHello to extract SNI hostname
- **TCP RST injection**: Kills blocked HTTPS connections before TLS handshake
- **Diagnostics counters**: Tracks packets processed, blocked, SNI blocks

### 6. ConnectionTracker (`engine/ConnectionTracker.kt`)
Maps network connections to their owning app packages.

- **Android 10+ (API 29+)**: Uses `ConnectivityManager.getConnectionOwnerUid()`
- **Android 9 and below**: Parses `/proc/net/tcp`, `/proc/net/udp`, `/proc/net/tcp6`, `/proc/net/udp6`
- **UID cache**: Pre-populates package name mapping for faster lookups
- **Periodic refresh**: Refreshes procfs table every 5 seconds on older devices

### 7. ArgusProxyServer (`engine/proxy/ArgusProxyServer.kt`)
Embedded HTTP/HTTPS proxy built with Kotlin coroutines (replaced LittleProxy/Netty for ~4MB APK savings).

- **Path-based blocking**: `/youtubei/v1/ads`, `/pagead`, `/api/stats/ads`
- **Host-based blocking**: Regex for `googleadservices.com`, `doubleclick.net`
- **User-Agent spoofing**: Modifies headers for YouTube requests
- **SOCKS5 mode**: Routes all TCP through local SOCKS5 for tun2socks integration
- **CA certificate**: On-device generated RSA 2048 CA using BouncyCastle

### 8. CaCertificateManager (`cert/CaCertificateManager.kt`)
Generates and manages the X.509 CA certificate.

- **On-device generation**: No pre-bundled keys — each device gets unique CA
- **BKS keystore**: Securely stores CA keypair in app-private storage
- **PEM export**: Saves `.crt` file for user installation via Settings
- **Leaf certificate generation**: Dynamically creates per-site certificates

### 9. BlocklistUpdateWorker (`data/worker/BlocklistUpdateWorker.kt`)
WorkManager periodic worker for daily blocklist updates.

- **Sources**: Firebog, OISD Big, AdGuard SDNS YouTube filter, StevenBlack hosts
- **Parsing**: Handles hosts file, AdGuard filter syntax, plain domain lists
- **Reload on update**: Automatically reloads DNS engine rules after successful update
- **Constraints**: Network required, battery not low

### 10. Background Resilience
- **BootReceiver**: Auto-starts VPN on boot if previously active
- **VpnWatchdogService**: 30-second health checks + AlarmManager exact alarm for Doze mode
- **BatteryOptimizationHelper**: Launches `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- **Notification actions**: Quick Stop and Restart buttons on persistent notification

---

## Build Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- An Android TV device or emulator (API 24+)

### Step-by-Step

1. **Open the project**
   Open the project root in Android Studio.

2. **Sync Gradle**
   Click "Sync Project with Gradle Files" in Android Studio.
   Dependencies: Hilt, Room, WorkManager, LittleProxy, BouncyCastle, dnsjava, OkHttp, OkHttp DoH.

3. **Build the APK**
   ```bash
   ./gradlew assembleDebug
   ```
   Or use Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**

4. **Install on Android TV**
   ```bash
   adb connect <TV_IP_ADDRESS>:5555
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

5. **First-time setup**
   - Launch **NexusBlock** from your TV's app drawer
   - Navigate to **Settings → Install CA Certificate** and follow instructions
   - The CA cert file: `/Android/data/com.nexusblock/files/nexusblock-ca.crt`
   - Go to **Android Settings → Security → Install from storage**
   - Return to NexusBlock, go to **Settings → Battery optimization**, and allow
   - Go to **Dashboard**, select **START BLOCKING**, grant VPN permission

6. **Verify it's working**
   ```bash
   adb logcat -s NexusBlock/DNS NexusBlock/Router NexusBlock/Proxy NexusBlock/VPN
   ```

---

## Testing Guide

### Manual Verification Checklist

| Test | Expected Result |
|------|----------------|
| Start blocking | VPN notification appears with Stop/Restart actions |
| Open YouTube | No preroll ads, no midroll interruptions |
| Browse web | No banner/popup ads on news sites |
| Check logs | Blocked domains appear in Logs tab |
| Add custom rule | Rule takes effect immediately |
| Change DNS mode | Upstream switches (Plain → DoH) without restart |
| Whitelist app | Selected app bypasses blocking |
| Reboot device | NexusBlock auto-starts if enabled |
| Network change | VPN reconnects within seconds |
| Battery optimization | App continues blocking after hours idle |
| Blocklist update | New domains loaded successfully |
| HTTPS MITM | Browser ad requests return 204 No Content |
| Diagnostics | Real-time packet stats update every second |

### ADB Commands for Debugging

```bash
# Real-time logs for all NexusBlock components
adb logcat -s NexusBlock/VPN NexusBlock/DNS NexusBlock/Router NexusBlock/Proxy NexusBlock/Watchdog NexusBlock/Rules NexusBlock/ConnTrack

# Check running services
adb shell dumpsys activity services | grep nexusblock

# Monitor network connections
adb shell netstat -tunapl | grep -E "53|8123"
```

---

## Recent Improvements

### System-Wide Indian OTT Ad Blocking
Massive expansion of built-in block rules targeting Server-Side Ad Insertion (SSAI) telemetry and programmatic ad networks used by Indian streaming services (Disney+ Hotstar, JioCinema, SonyLIV, ZEE5, MX Player, etc.).

### Advanced YouTube Ad Blocking
- **DNS Sinkholing & QUIC Blocking:** Blocks HTTPS and SVCB records for ad domains to prevent apps from discovering QUIC (HTTP/3) endpoints, forcing fallback to TCP where SNI inspection works.
- **Video Server Pattern Matching:** Dynamic regex matching in the RuleEngine catches `googlevideo.com` ad-serving subdomains without breaking primary video playback.
- **IPv6 Null-Routing:** Prevents DNS leaks on dual-stack Android TVs by null-routing global unicast IPv6, forcing all DNS queries through the local IPv4 DNS interceptor.

### AdGuard-Style Rule Engine
Replaced the simple HashSet+Trie approach with a full Adblock-style rule parser supporting `||`, `@@||`, `|`, regex, and hosts syntax. This makes NexusBlock compatible with nearly all public filter lists.

### Per-App Connection Tracking
Inspired by RethinkDNS, we now track which app owns each network connection:
- Modern devices: `ConnectivityManager.getConnectionOwnerUid()`
- Legacy devices: `/proc/net/tcp` and `/proc/net/udp` parsing
- Enables per-app statistics and smarter whitelisting

### DNS-over-HTTPS Support
Upstream DNS can now use encrypted DoH resolvers (Cloudflare, Google, Quad9) instead of plain UDP. This prevents ISPs from snooping on DNS queries and improves privacy.

### Custom Rules Editor
Users can now add their own blocking or allow rules directly from the TV UI. Supports the same AdGuard syntax as imported blocklists.

### Diagnostics Screen
Real-time display of:
- DNS queries processed, blocked, allowed
- Total packets processed and blocked
- SNI-based blocked connections
- Current DNS upstream mode

### Notification Quick Actions
The persistent VPN notification now includes **Stop** and **Restart** buttons, allowing quick control without opening the app.

---

## Optimizations & Hacks

### 1. Three-Tier Domain Matching + Trie
Instead of scanning a list, we use:
- **HashSet** for exact matches (O(1))
- **Trie** for subdomain wildcard matching
- **Regex** list for dynamic patterns

Millions of domains checked in microseconds.

### 2. SNI Inspection (Certificate Pinning Bypass)
Apps like YouTube pin certificates → MITM fails. Solution:
- Parse unencrypted TLS ClientHello in VPN packet stream
- Extract SNI hostname
- If blocked → send TCP RST before any encrypted data
- No decryption needed → bypasses pinning entirely

### 3. NXDOMAIN Instead of Drops
Blocked DNS queries get a proper **NXDOMAIN response** with matching transaction ID. This eliminates timeout delays and speeds up page loads.

### 4. Batch Event Logging
- Events queued in memory `MutableList`
- Coroutine flushes batch to Room every **5 seconds**
- Reduces disk I/O by 100x+ under heavy load

### 5. WorkManager with Constraints
Blocklist updates use `setRequiresBatteryNotLow(true)` and `setRequiredNetworkType(UNMETERED)` to avoid draining battery or consuming mobile data.

### 6. YouTube-Specific Rules
Curated list beyond generic blocklists:
- `/youtubei/v1/ads` — empty 204 response at proxy layer
- `/youtubei/v1/log_event` — prevents ad telemetry
- `googlevideo.com/videoplayback.*&oad` — blocks ad video segments
- Dynamic `googlevideo` subdomain regex patterns
- User-Agent spoofing for YouTube endpoints

---

## Limitations & Workarounds

### 1. Certificate Pinning
**Problem**: YouTube, Netflix, Spotify, banking apps pin certificates.
**Workaround**: SNI inspection at VPN layer blocks ad domains before TLS handshake. MITM only effective for browsers and unpinned apps.

### 2. Apps Using Private DNS (DoH/DoT)
**Problem**: Some apps use DNS-over-HTTPS directly, bypassing local DNS.
**Workaround**: VPN captures ALL traffic, so DoH requests still route through TUN. SNI inspection catches ad requests regardless of DNS method.

### 3. IPv6-Only Networks
**Problem**: Some ISPs are IPv6-only.
**Current status**: IPv6 routes added to TUN (`::/0`), but DNS and SNI are primarily IPv4-focused. Full IPv6 packet parsing in `PacketRouter` is a future enhancement.

### 4. Google Play Store Rejection
**Problem**: Play Store policies prohibit interfering with other apps.
**Workaround**: Distribute via direct APK sideload, F-Droid, or third-party stores.

### 5. YouTube Anti-Adblock Arms Race
**Problem**: YouTube changes ad delivery domains frequently.
**Workaround**: Daily blocklist updates, SNI inspection (harder to evade than DNS-only), regex-based dynamic subdomain rules, community rule submissions.

---

## Future Improvements

1. **Machine Learning Ad Detection** — Train a lightweight TensorFlow Lite model on request features (payload size, timing, frequency).

2. **Stealth / Obfuscation Mode** — Wrap VPN traffic in HTTP CONNECT tunnels or apply traffic shaping.

3. **Cloud Sync** — Firebase Auth + Firestore for settings/rules sync across devices.

4. **Split Tunneling v2** — Per-app rule configuration (DNS-only, full MITM, or bypass).

5. **Live Blocklist Community** — Submit newly discovered ad domains from the Logs screen.

6. **Full IPv6 Support** — Extend `PacketRouter` to parse IPv6 extension headers.

7. **HTTP/2 Proxy Support** — Upgrade MITM proxy to handle HTTP/2 streams.

8. **A/B Testing Framework** — Remote config-driven experiments for blocking strategies.

---

## License

This project is provided as an educational reference for VPN-based ad blocking architecture on Android.

---

## Acknowledgments

- **dnsjava** — DNS protocol parsing
- **ArgusProxyServer** — Lightweight Kotlin coroutine HTTP/HTTPS proxy
- **BouncyCastle** — Cryptographic certificate generation
- **AdGuard** — Inspiration for DNS filtering and rule syntax
- **RethinkDNS** — Inspiration for connection tracking and UI philosophy
- **StevenBlack, Firebog, OISD** — Community blocklist curation
