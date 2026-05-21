# Sprint 1 Integration Test Plan
## Argus Ad Blocker — DNS + SNI + MITM API Rewriter MVP

### Build Verification ✅
```bash
./gradlew clean assembleDebug
./gradlew testDebugUnitTest
```
- **Status:** All 31 unit tests pass. Debug APK builds successfully (~20 MB per ABI).

---

## Test Environment Setup

### Required Hardware
- Android TV box or emulator (API 24–36)
- ADB access over Wi-Fi or USB
- Root access OPTIONAL (for system CA testing)

### Required Accounts
- YouTube account (to test logged-in ad blocking)
- Spotify Free account (to test audio ad blocking)

---

## Test Cases

### TC-1: VPN Service Startup & DNS Filtering
**Objective:** Verify Layer 1 (DNS) blocks ad domains system-wide.

| Step | Action | Expected Result |
|---|---|---|
| 1 | Install APK via ADB: `adb install app/build/outputs/apk/debug/app-universal-debug.apk` | App installs successfully |
| 2 | Launch app → Dashboard → tap "Start Blocking" | VPN permission dialog appears |
| 3 | Grant VPN permission | Service starts, notification shows "Argus is active" |
| 4 | Open browser/WebView app, navigate to `https://doubleclick.net` | Connection fails / page unreachable |
| 5 | Navigate to `https://youtube.com` | Page loads normally (whitelisted) |
| 6 | Check Dashboard → "Active Rules" counter | Shows >100,000 rules loaded |

**Pass Criteria:** DNS blocking works, non-blocked sites load normally.

---

### TC-2: QUIC SNI Inspection
**Objective:** Verify QUIC (HTTP/3) connections are inspected for SNI and blocked.

| Step | Action | Expected Result |
|---|---|---|
| 1 | Start Argus VPN | Service active |
| 2 | Open YouTube app on Android TV | Videos load |
| 3 | Using `adb logcat -s Argus/Router`, observe UDP/443 traffic | Log shows "QUIC SNI block:" for blocked domains |
| 4 | Ensure `googleadservices.com` QUIC connections are dropped | No ad metadata reaches the client |

**Pass Criteria:** QUIC SNI blocks appear in logs; no crash or ANR.

---

### TC-3: YouTube MITM Ad Stripping (Layer 2 MVP)
**Objective:** Verify decrypted youtubei.googleapis.com responses have ads removed.

**Prerequisites:**
- User must manually install Argus CA certificate:
  1. Settings → Security → Install from device storage
  2. Select `nexusblock-ca.crt` from Downloads
  3. Set name to "Argus CA"
- In Argus app: enable "MITM Proxy" toggle in Dashboard

| Step | Action | Expected Result |
|---|---|---|
| 1 | Install CA cert as above | VPN notification shows Argus active |
| 2 | Open YouTube TV app | App launches, no SSL error |
| 3 | Play any video | Preroll ad SKIPPED or absent |
| 4 | Check `adb logcat -s Argus/Transform` | Log shows "YouTube response stripped ads" |
| 5 | Seek to mid-video ad break (if any) | No midroll ad plays |
| 6 | Browse YouTube home → search | Recommendations load normally (not broken) |

**Known Limitations (Sprint 1):**
- Some SSAI (server-side injected) ads may still play; these are handled in Sprint 2 via Accessibility.
- YouTube may show black screen for 1-2s where ad was removed (acceptable vs. watching full ad).

**Pass Criteria:** 70%+ of video ads blocked without breaking YouTube functionality.

---

### TC-4: Spotify MITM Ad Stripping
**Objective:** Verify Spotify audio ads are blocked via API response rewriting.

**Prerequisites:** Same CA cert installation as TC-3.

| Step | Action | Expected Result |
|---|---|---|
| 1 | Open Spotify app (Free tier) | App launches, no SSL error |
| 2 | Play any playlist or radio | Music starts immediately |
| 3 | Listen through 3-5 songs | NO audio ads interrupt playback |
| 4 | Check `adb logcat -s Argus/Transform` | Shows "Spotify response stripped" entries |
| 5 | Check artist page | No promotional banners injected |

**Pass Criteria:** Audio ads blocked in 80%+ of sessions.

---

### TC-5: Strategy Router App Detection
**Objective:** Verify foreground app detection and per-app strategy loading.

| Step | Action | Expected Result |
|---|---|---|
| 1 | Enable Usage Stats permission for Argus in TV Settings | App has permission |
| 2 | Start Argus VPN + enable Strategy Router | Router starts (log: "StrategyRouter started") |
| 3 | Open YouTube TV | Log shows: "Foreground: com.google.android.youtube.tv profile=YouTube TV mitm=true" |
| 4 | Switch to Netflix | Log shows: "Foreground: com.netflix.ninja profile=Netflix mitm=false" |
| 5 | Return to Dashboard → App Strategy | Shows correct profiles per app |

**Pass Criteria:** App detection works within 5 seconds of foreground switch.

---

### TC-6: Performance Benchmark
**Objective:** Verify CPU and memory targets on reference hardware.

| Metric | Target | Measurement Method |
|---|---|---|
| CPU usage (VPN active, idle) | < 5% | `adb shell top -p $(pidof com.nexusblock)` |
| CPU usage (YouTube playback) | < 10% | Same as above |
| RAM footprint | < 150 MB | `adb shell dumpsys meminfo com.nexusblock` |
| Master toggle latency | < 300 ms | Manual stopwatch (tap toggle → notification change) |
| DNS query latency | < 50 ms avg | Custom benchmark script |

**Pass Criteria:** All metrics within targets.

---

### TC-7: No-Root CA Cert Onboarding
**Objective:** Verify non-root user can install CA and enable MITM.

| Step | Action | Expected Result |
|---|---|---|
| 1 | Fresh install on non-rooted device | Onboarding wizard launches |
| 2 | Follow VPN permission step | VPN starts |
| 3 | Follow CA installation step | System file picker opens with `nexusblock-ca.crt` |
| 4 | Install cert, name it "Argus CA" | Cert appears in Settings → Security → Trusted credentials (User) |
| 5 | Return to app, verify MITM toggle enabled | Toggle is ON and persists |

**Pass Criteria:** User completes onboarding in < 2 minutes without root.

---

## Regression Tests

| Test | Description |
|---|---|
| R-1 | Legacy DNS-only mode still works when MITM is disabled |
| R-2 | App firewall whitelist/bypass still functions |
| R-3 | Blocklist auto-update Worker still runs on schedule |
| R-4 | BootReceiver auto-starts VPN when enabled |
| R-5 | IPv6 DNS queries return synthetic sinkholes (no crash) |

---

## Defect Triage Categories

| Severity | Criteria | Action |
|---|---|---|
| P0 — Blocker | Crash on startup, VPN fails to establish, all apps lose internet | Stop sprint, fix immediately |
| P1 — Critical | MITM breaks target app (YouTube/Spotify won't launch), ads not blocked at all | Must fix before Sprint 2 |
| P2 — Major | Some ads leak through, performance >15% CPU | Document for Sprint 2 |
| P3 — Minor | UI glitches, log noise, missing translations | Backlog |

---

## Sign-Off Checklist

- [ ] TC-1 DNS Filtering passes
- [ ] TC-3 YouTube MITM passes (with CA cert installed)
- [ ] TC-4 Spotify MITM passes (with CA cert installed)
- [ ] TC-6 Performance meets targets
- [ ] All 31 unit tests pass
- [ ] Debug APK installs and runs on physical Android TV device
- [ ] No P0 or P1 defects outstanding

Once signed off, proceed to **Sprint 2: Accessibility + Audio "Eyes" (SSAI Killer)**.
