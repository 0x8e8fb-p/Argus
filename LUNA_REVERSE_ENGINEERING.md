# Luna AdBlocker Reverse Engineering Report

## APK Analyzed
- **File**: `Click_me_to_install_Adblock-Mobile-7.10.3.1567.apk`
- **Size**: 16 MB
- **Package**: `com.babybluewireless.android` (also references `com.gibli.vpn`)
- **Architecture**: Standard Android VpnService + strongSwan IPsec native VPN

## Key Findings

### 1. Architecture Overview
Luna uses a **layered approach** to ad blocking:

```
Native Layer (C/C++):
  - libstrongswan.so, libcharon.so, libipsec.so (IPsec VPN tunnel)
  - libandroidbridge.so (JNI bridge to Android VpnService)

Java/Kotlin Layer:
  - VpnService subclass (standard Android VPN)
  - DomainFilter (DNS/hosts-based blocking)
  - AdblockTestWebBrowser (in-app browser for testing)

WebView Layer:
  - YouTube IFrame API player with injected adblock vars
  - Fake launcher icons for popular apps (YouTube, Instagram, Twitter, etc.)
```

### 2. Native VPN (strongSwan)
Unlike NexusBlock's pure Kotlin/Java approach, Luna includes **strongSwan IPsec** native libraries across all architectures (arm64-v8a, armeabi-v7a, x86, x86_64). This provides a more robust VPN tunnel but significantly increases APK size (~5-8MB just for natives).

**NexusBlock decision**: Keep standard VpnService (simpler, smaller APK, sufficient for DNS blocking). Note strongSwan as future enhancement.

### 3. YouTube Strategy (CRITICAL INSIGHT)
Luna's YouTube blocking is **multi-pronged**:

#### a) WebView YouTube Player (`res/raw/ayp_youtube_player.html`)
- Uses YouTube IFrame API (`https://www.youtube.com/iframe_api`)
- Injects `<<injectedPlayerVars>>` to configure ad-free playback
- Has JavaScript bridge (`YouTubePlayerBridge`) communicating with native code
- Luna detects that the native YouTube app uses certificate pinning
- Instead of fighting it, they provide an **alternative player**

**Implemented in NexusBlock**: `YouTubePlayerScreen.kt` with identical IFrame API approach + injected playerVars + ad-skip JavaScript

#### b) Fake Launcher Icons (App Integration)
Luna creates launcher shortcuts disguised as popular app icons:
- `adblock_youtube_foreground`, `adblock_youtube_launcher`
- `adblock_instagram_foreground`, `adblock_instagram_launcher`
- `adblock_twitter_foreground`, `adblock_twitter_launcher`
- `adblock_reddit_foreground`, `adblock_reddit_launcher`
- `adblock_spotify_foreground`, `adblock_spotify_launcher`
- `adblock_twitch_foreground`, `adblock_twitch_launcher`
- `adblock_pinterest_foreground`, `adblock_pinterest_launcher`
- `adblock_soundcloud_foreground`, `adblock_soundcloud_launcher`
- `adblock_nyt_foreground`, `adblock_nyt_launcher`

These shortcuts open apps through Luna's ad-blocked WebView/proxy instead of the original app!

**NexusBlock decision**: Quick Launch shortcuts could be added in future. Current priority is system-wide VPN blocking.

#### c) YouTube Checklist / Tutorial
Luna has dedicated onboarding:
- `core_youtube_checklist_title`
- `core_youtube_checklist_title_optimize`
- `core_youtube_notification_content_adblock`
- `core_tutorial_youtube_adblock`

**Implemented in NexusBlock**: `SetupChecklistScreen.kt` with YouTube setup step

### 4. Blocklist Infrastructure
- **Hosting**: `s3-us-west-1.amazonaws.com/adbl0ck/`
- **Domain**: `adbl0ck.com` (also `vpn-android.adbl0ck.com`)
- **Backend API**: `trymobilevpn.com/gen2/`
  - `/ca-install` — CA certificate installation guide
  - `/flags-android` — Feature flags
  - `/luna/branch_upgrade` — Version update checks
  - `/upload-logs` — Telemetry upload
  - `/android-versions` — APK version check

**NexusBlock decision**: Keep GitHub-based blocklists + direct downloads from Firebog/OISD/AdGuard. Add S3 hosting as future option.

### 5. DNS/Filtering Approach
- `DomainFilter` class found in DEX
- `hosts` file references found
- Standard `VpnService` with `addAddress`, `addRoute`, `establish`
- No custom proxy server found in strings (unlike our LittleProxy approach)
- Appears to be primarily **DNS-based blocking** with hosts file + domain list

**NexusBlock advantage**: We combine DNS blocking + SNI inspection + MITM proxy (3 layers vs Luna's ~2)

### 6. Certificate Management
- `adblock_ca_cert.pem` — CA certificate file
- `core_ca_intro_1`, `core_ca_intro_2` — CA installation tutorial screens
- `core_ca_pager_disclaimer_1` — Disclaimer about installing CA cert
- Network security config trusts user certificates (`src="user"`)

**NexusBlock equivalent**: `CertificateManager.kt` with BouncyCastle CA generation + Settings screen integration

### 7. In-App Browser
- `AdblockTestWebBrowser` — Dedicated browser for testing ad blocking
- `core_activity_browser` — Browser activity
- `core_adblock_test_description` — Test instructions
- `core_adblock_test_return_to_app_ads` / `no_ads` — Test result messages

**Implemented in NexusBlock**: `AdFreeBrowserScreen.kt` with WebView URL interception

### 8. Gamification / Retention
Luna has game-like features:
- `GameFeature`, `GameTier` database entities
- `core_tutorial_game`
- `core_trophy_bronze`, `core_trophy_silver`, `core_trophy_gold`, `core_trophy_platinum`, `core_trophy_diamond`
- `core_unlocked_features`

**NexusBlock decision**: Not implementing gamification. Focus on core ad-blocking functionality.

### 9. Analytics Stack (Heavy)
Luna includes extensive analytics:
- Firebase Analytics, Crashlytics, Messaging
- Facebook App Events, Graph API
- Amplitude (`api.amplitude.com`)
- SensorTower panel (`sensortower.com`)
- AppsFlyer
- Branch
- Pusher
- Helpscout Beacon

**NexusBlock decision**: No analytics. Privacy-first approach.

### 10. Subscription / Monetization
- `core_activity_membership_status`
- `SubscriptionExpirationPreference`
- `com.android.vending.billing.IInAppBillingService` (Google Play Billing)
- `com.android.billingclient.api.BillingClient`

**NexusBlock decision**: Open-source, no subscriptions.

## Implementable Techniques Applied to NexusBlock

| Luna Feature | NexusBlock Implementation |
|--------------|---------------------------|
| YouTube IFrame WebView Player | `YouTubePlayerScreen.kt` + `YouTubePlayerViewModel.kt` |
| In-App Ad-Free Browser | `AdFreeBrowserScreen.kt` + `AdFreeBrowserViewModel.kt` |
| Setup Checklist / Tutorial | `SetupChecklistScreen.kt` + `SetupChecklistViewModel.kt` |
| Fake App Launcher Icons | Quick Launch shortcuts (future enhancement) |
| DomainFilter (DNS/hosts) | `DnsFilterEngine.kt` + `RuleEngine.kt` (already superior) |
| strongSwan IPsec Native VPN | Documented as limitation; standard VpnService used |
| Blocklist CDN (S3) | GitHub + direct downloads (S3 could be added) |
| CA Certificate Management | `CertificateManager.kt` (already implemented) |
| YouTube Checklist | Integrated into `SetupChecklistScreen` |

## Advantages NexusBlock Has Over Luna

1. **Open source** — Luna is closed-source; NexusBlock is fully transparent
2. **No analytics/spyware** — Luna has 7+ analytics SDKs; NexusBlock has zero
3. **No subscription** — Luna is freemium; NexusBlock is free
4. **MITM proxy** — Luna appears DNS-only; NexusBlock adds SNI + HTTPS proxy
5. **Custom rules** — Luna may not support user rules; NexusBlock has full editor
6. **DoH upstream** — Luna likely uses plain DNS; NexusBlock supports encrypted DNS
7. **TV-optimized UI** — Luna is phone-focused; NexusBlock is built for Android TV
8. **Real-time diagnostics** — Luna has stats; NexusBlock has live packet-level diagnostics
