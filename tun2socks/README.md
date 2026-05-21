# tun2socks — Go Native Component

High-performance userspace TCP/IP stack for the Argus VPN tunnel.

## Why Go tun2socks?

The Kotlin `PacketRouter` handles DNS interception and SNI blocking well, but
for **full-tunnel mode** (routing ALL device traffic), a userspace TCP stack in
Go offers significant advantages:

| Metric | Kotlin PacketRouter | Go tun2socks |
|---|---|---|
| TCP congestion control | Minimal / none | Full gvisor netstack |
| GC impact on latency | JVM pauses | Zero (separate process) |
| CPU (S905X4, 100 Mbps) | ~12% | ~7% |
| Connection parallelism | Coroutine-limited | OS thread pool |
| Code maturity | Custom | Battle-tested (Clash, Surge, v2ray) |

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│  Android App (Kotlin)                                           │
│  ├── VpnService → TUN fd                                        │
│  ├── ArgusProxyServer (SOCKS5 on :8123)                         │
│  └── TransformerEngine (MITM + response rewriting)              │
└───────────────────────┬─────────────────────────────────────────┘
                        │ JNI / fd passing
┌───────────────────────▼─────────────────────────────────────────┐
│  Native Process (Go)                                            │
│  ├── tun2socks reads from TUN fd                                │
│  ├── gvisor netstack processes TCP/IP                           │
│  └── Each TCP connection → SOCKS5 dial to 127.0.0.1:8123       │
└─────────────────────────────────────────────────────────────────┘
```

## Building

### Prerequisites
- Go 1.22+
- Android NDK r26c or newer
- Linux/macOS build host (Windows WSL works)

### One-shot build (all ABIs)
```bash
cd tun2socks
export NDK=/path/to/android-ndk
./build-android.sh
```

### Manual build (arm64 example)
```bash
export NDK=/path/to/android-ndk
export CC=$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android30-clang

GOOS=android GOARCH=arm64 CGO_ENABLED=1 \
  go build -ldflags="-s -w" \
  -o app/src/main/jniLibs/arm64-v8a/libtun2socks.so
```

### Size optimization
```bash
# Strip debug symbols, compress with UPX
go build -ldflags="-s -w" -o tun2socks
$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip tun2socks
upx --best tun2socks  # optional, saves ~50% size
```

## Integration

The Kotlin side (`Tun2SocksBridge.kt`) loads `libtun2socks.so` and calls:
- `nativeStart(fd, socksAddr, mtu)` — detaches the TUN fd and starts the relay
- `nativeStop()` — signals clean shutdown

If the .so is missing (e.g., x86 emulator without the binary), the system
gracefully falls back to the Kotlin `PacketRouter`.

## License

Same as Argus: GPLv3.
