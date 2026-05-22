# ArgusBlock — Ad Blocker for Android TV

System-wide ad and tracker blocking for every Android TV device. No root required.

ArgusBlock creates a local VPN tunnel to intercept DNS queries and block advertising/tracking domains before they reach your apps. All processing happens on-device — no external servers, no data collection.

---

## Features

| Category | Details |
|----------|---------|
| **DNS Filtering** | Blocks ad/tracker domains via curated blocklists (AdGuard, OISD, HaGeZi, StevenBlack, and more) |
| **Encrypted DNS** | DoH (DNS-over-HTTPS) and DoT (DNS-over-TLS) upstream resolution |
| **SNI Inspection** | Detects ad-serving hosts on encrypted HTTPS connections |
| **Per-App Firewall** | VPN, Bypass, or DNS-only mode per installed app |
| **Custom Rules** | Add your own block/allow rules (AdGuard syntax, hosts format, regex) |
| **Auto-Start** | Resumes protection automatically after TV reboots |
| **Zero-Config** | Works out of the box with sensible defaults |
| **TV-Optimized UI** | Full D-pad navigation, responsive layout for 480p–4K displays |

---

## UI Overview

**Home** — Large shield status indicator with one-tap protection toggle and real-time stats (ads blocked, data saved, active rules).

**Activity** — Live feed of blocked DNS/SNI events with timestamps and domain names.

**Settings** — General options (DNS provider, auto-start, battery optimization) plus an Advanced Configuration panel containing:
- Blocking techniques (DNS filtering, SNI watch, IP blocking, stealth mode, etc.)
- Blocklist management (14 curated sources)
- Custom rules editor
- Per-app firewall controls

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + AndroidX TV Material 3
- **Architecture:** MVVM (ViewModels + StateFlow)
- **DI:** Hilt
- **Storage:** Room (blocked events, custom rules) + DataStore (preferences)
- **Networking:** OkHttp (DoH/DoT upstream), raw socket packet routing
- **Background:** WorkManager + Foreground Service (VPN)
- **Font:** Dubai (4 weights)

---

## Build

```bash
# Debug build
./gradlew assembleDebug

# Release AAB
./gradlew bundleRelease

# Install on connected TV
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Requirements:**
- JDK 17+
- Android SDK 36 (compile) / min SDK 24
- Gradle 8.9+

---

## Install on Android TV

1. Enable **Developer Options** on your TV (Settings → About → tap Build Number 7 times)
2. Enable **USB Debugging** or **ADB over Network**
3. Sideload the APK:
   ```bash
   adb connect <tv-ip>:5555
   adb install argusblock.apk
   ```
4. Launch ArgusBlock from the TV home screen
5. Approve the VPN permission dialog when prompted

---

## Project Structure

```
app/src/main/java/com/nexusblock/
├── ui/                    # Compose UI layer
│   ├── screens/           # Home, Settings, Logs, AdvancedSettings
│   ├── components/        # Reusable TV components
│   ├── theme/             # Colors, typography, responsive dimensions
│   └── viewmodel/         # MVVM state holders
├── service/               # VPN service, watchdog, boot receiver
├── engine/                # DNS filter, packet router, rule engine
├── data/                  # Room DB, repositories, models
└── di/                    # Hilt modules
```

---

## Supported DNS Providers

AdGuard (Standard/Family) · Cloudflare (1.1.1.1/Security/Family) · CleanBrowsing · NextDNS · ControlD · Quad9 · Google DNS

---

## License

Private repository. All rights reserved.
