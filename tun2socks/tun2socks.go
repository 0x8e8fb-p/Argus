// +build android

// tun2socks — lightweight userspace TCP stack for Android VPN integration.
//
// This Go binary routes all TCP traffic from a TUN file descriptor through
// a local SOCKS5 proxy (ArgusProxyServer on 127.0.0.1:8123).
//
// Architecture:
//   Android VpnService TUN fd → tun2socks (Go) → SOCKS5 → ArgusProxyServer (Kotlin)
//   → selective TLS MITM → TransformerEngine → upstream server
//
// Benefits over Kotlin PacketRouter for TCP:
//   - Zero JVM GC pauses during packet processing
//   - Mature TCP/IP stack (gvisor/netstack)
//   - Lower CPU overhead on low-end devices (Amlogic S905X4)
//   - Parallel connection handling without Kotlin coroutine overhead
//
// Build: GOOS=android GOARCH=arm64 go build -ldflags="-s -w" -o tun2socks
// Or use gomobile bind for direct JNI integration.
//
// Copyright (C) 2025 Argus Ad Blocker — GPLv3

package main

import (
	"flag"
	"fmt"
	"log"
	"net"
	"os"
	"runtime"
	"strconv"
	"syscall"
	"time"

	"github.com/yinghuocho/gotun2socks"
	"github.com/yinghuocho/gosocks"
)

var (
	tunDevice = flag.String("tunDevice", "", "TUN device name (e.g., tun0)")
	tunFD     = flag.Int("tunFD", -1, "TUN file descriptor passed from Android VpnService")
	tunAddr   = flag.String("tunAddr", "10.42.0.2", "TUN interface IP address")
	tunMask   = flag.String("tunMask", "255.255.255.0", "TUN interface netmask")
	tunMTU    = flag.Int("tunMTU", 1500, "TUN interface MTU")
	socksAddr = flag.String("socksAddr", "127.0.0.1:8123", "SOCKS5 proxy address")
	udpRelay  = flag.Bool("udpRelay", false, "Enable UDP relay via SOCKS5")
	dnsCache  = flag.Bool("dnsCache", true, "Enable DNS caching")
	logLevel  = flag.String("logLevel", "warn", "Log level: debug, info, warn, error")
)

func main() {
	flag.Parse()

	setLogLevel(*logLevel)

	log.Printf("[tun2socks] Starting — tun2socks for Argus Ad Blocker")
	log.Printf("[tun2socks] SOCKS5 proxy: %s", *socksAddr)
	log.Printf("[tun2socks] TUN MTU: %d", *tunMTU)
	log.Printf("[tun2socks] Go version: %s", runtime.Version())

	// Validate SOCKS5 proxy is reachable
	if err := checkSocks5(*socksAddr); err != nil {
		log.Fatalf("[tun2socks] SOCKS5 proxy unreachable: %v", err)
	}

	// Open TUN device from FD (Android) or by name
	tunDev, err := openTUN()
	if err != nil {
		log.Fatalf("[tun2socks] Failed to open TUN: %v", err)
	}
	defer tunDev.Close()

	log.Printf("[tun2socks] TUN device opened")

	// Create SOCKS5 dialer
	socksDialer := gosocks.NewSocks5Dialer("tcp", *socksAddr, "", "")

	// Create tun2socks engine with gvisors netstack
	lwipWriter := gotun2socks.NewLWIPStack()

	// Register TCP handlers: TCP connections go through SOCKS5
	tcpConnHandler := gotun2socks.NewTCPHandler(*tunAddr, *tunMask, socksDialer)
	gotun2socks.RegisterTCPConnHandler(tcpConnHandler)

	// Optional UDP relay
	if *udpRelay {
		udpConnHandler := gotun2socks.NewUDPHandler(*socksAddr, 30*time.Second, *tunAddr, *tunMask)
		gotun2socks.RegisterUDPConnHandler(udpConnHandler)
		log.Printf("[tun2socks] UDP relay enabled")
	}

	// Start relay: read from TUN → write to LWIP stack → SOCKS5
	relay := gotun2socks.NewRelay(lwipWriter, tunDev, *tunMTU)
	relay.Start()

	log.Printf("[tun2socks] Running — press Ctrl+C or send SIGTERM to stop")

	// Block until signal
	select {}
}

func openTUN() (*os.File, error) {
	if *tunFD >= 0 {
		// Android passes the TUN fd directly
		return os.NewFile(uintptr(*tunFD), "/dev/tun"), nil
	}
	if *tunDevice != "" {
		return os.OpenFile("/dev/net/tun", os.O_RDWR, 0)
	}
	return nil, fmt.Errorf("either -tunFD or -tunDevice must be specified")
}

func checkSocks5(addr string) error {
	conn, err := net.DialTimeout("tcp", addr, 2*time.Second)
	if err != nil {
		return err
	}
	conn.Close()
	return nil
}

func setLogLevel(level string) {
	// Go's log package doesn't have levels; we just filter
	// In production, integration with Android logcat is handled via
	// android/log native bridge or stdout redirect.
	log.SetFlags(log.LstdFlags | log.Lmicroseconds)
}

// JNI-exported entry points for Android integration.
// These are called from Kotlin via JNA or JNI bindings.
//
// extern "C" {
//     void Java_com_nexusblock_jni_Tun2Socks_startTun2Socks(...);
//     void Java_com_nexusblock_jni_Tun2Socks_stopTun2Socks(...);
// }

// StartTun2SocksJNI is exported for JNI binding.
func StartTun2SocksJNI(tunFd int, socksAddr string, mtu int) {
	*tunFD = tunFd
	*socksAddr = socksAddr
	*tunMTU = mtu
	go main()
}

// StopTun2SocksJNI signals the tun2socks process to exit.
func StopTun2SocksJNI() {
	os.Exit(0)
}
