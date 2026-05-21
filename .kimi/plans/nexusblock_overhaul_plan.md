# NexusBlock Overhaul Plan — Indian OTT + YouTube Adblocking

## Root Cause Analysis

1. **VPN only routes DNS traffic** (`addRoute("10.0.0.1", 32)`). TCP/UDP traffic to real IPs bypasses TUN entirely.
2. **SNI inspection only works on traffic in TUN**, so YouTube `googlevideo.com` ad detection at SNI level is INEFFECTIVE for native app.
3. **UI uses default fonts**, not Dubai as requested.
4. **Missing screens**: YouTubePlayerScreen, AdFreeBrowserScreen not present.
5. **Limited googlevideo.com ad patterns** — only checks `-ad-`, `redirector.`, `---ad`, `_ad_`.

## Execution Plan

### Phase 1: Full-Tunnel Packet Router (CRITICAL)
- Add `addRoute("0.0.0.0", 0)` to capture ALL device traffic
- Implement TCP NAT forwarder in PacketRouter:
  - For TCP SYN to blocked SNI → inject RST, drop
  - For TCP SYN to allowed destinations → open `VpnService.protect()` socket, spawn relay coroutine
- Implement UDP forwarder for non-DNS traffic
- This makes SNI inspection actually work for YouTube native app

### Phase 2: Enhanced Ad Detection
- Expand `isAdVideoServer()` with comprehensive googlevideo.com ad patterns
- Add IP-range based blocking for known ad CDNs
- Add more Indian OTT ad domains and update blocklists

### Phase 3: YouTube IFrame Player
- Create `YouTubePlayerScreen.kt` with WebView + YouTube IFrame API
- JS injection to skip ads (`youtube.ajax`, playerVars manipulation)
- Provide launcher shortcut so users can use ad-free player

### Phase 4: UI Overhaul with Dubai Font
- Download Dubai font TTF
- Register in `res/font/` and create FontFamily
- Update `Type.kt` to use Dubai throughout
- Polish all screens for Android TV (focus handling, navigation)

### Phase 5: Build & Deploy
- Build debug APK
- ADB install to Lumio Vision TV
- Smoke test VPN start, ad blocking
- Iterate fixes
