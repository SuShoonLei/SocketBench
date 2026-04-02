# 🌐 TCP & UDP Network Performance Benchmarking Tool

A Java-based network benchmarking suite that measures and compares the performance of **TCP** and **UDP** protocols through real-time **RTT (Round-Trip Time)** and **throughput** analysis across multiple payload sizes.

---

## 📋 Overview

This project implements a client-server architecture for both TCP and UDP protocols to empirically measure network performance. Each protocol is tested with varying payload sizes, and results are exported as structured JSON for visualization and analysis.

The suite includes optional **XOR-shift stream encryption** applied to all transmitted payloads, simulating lightweight data obfuscation in a real network environment.

---

## ✨ Features

- **Dual-protocol support** — Independent TCP and UDP client/server pairs
- **RTT Benchmarking** — Average round-trip time measured over 1,000 messages per payload size
- **Throughput Benchmarking** — Sustained data transfer rate in Mbps
- **Warm-up phase** — 100 pre-test messages to stabilize JVM and network stack before measurements
- **XOR-shift encryption** — Optional lightweight payload encryption/decryption on both ends
- **JSON result export** — Results are appended to `web/plot_results.json` for visualization
- **Multiple payload sizes** — Tests run across 8B, 64B, 256B, and 512B payloads

---

## 🗂️ Project Structure

```
├── TcpClient.java       # TCP client: sends payloads, measures RTT & throughput
├── TcpServer.java       # TCP server: echoes received payloads back to client
├── UdpClient.java       # UDP client: sends datagrams, handles packet loss
├── UdpServer.java       # UDP server: echoes received datagrams back to client
└── web/
    └── plot_results.json  # Auto-generated benchmark output (JSON)
```

---

## 🚀 Getting Started

### Prerequisites

- Java 11 or higher
- No external dependencies required

### Compile

```bash
javac TcpServer.java TcpClient.java UdpServer.java UdpClient.java
```

### Run TCP Benchmark

```bash
# Terminal 1 — Start the TCP server
java TcpServer

# Terminal 2 — Run the TCP client
java TcpClient
```

### Run UDP Benchmark

```bash
# Terminal 1 — Start the UDP server
java UdpServer

# Terminal 2 — Run the UDP client
java UdpClient
```

---

## ⚙️ Configuration

All parameters are defined as constants at the top of each client class:

| Constant        | Default       | Description                              |
|-----------------|---------------|------------------------------------------|
| `HOST`          | `localhost`   | Target server hostname or IP             |
| `PORT`          | `5020` / `6000` | Server port (TCP / UDP)                |
| `SIZES`         | `{8, 64, 256, 512}` | Payload sizes in bytes to test     |
| `NUM_MESSAGES`  | `1000`        | Number of messages per test              |
| `ENCRYPT`       | `true`        | Enable XOR-shift payload encryption      |
| `TIMEOUT_MS`    | `3000` *(UDP only)* | Socket receive timeout in ms       |

---

## 📊 Sample Output

**Console:**
```
Size=8B   | Avg RTT=0.124 ms | Throughput=0.843 Mbps
Size=64B  | Avg RTT=0.131 ms | Throughput=6.201 Mbps
Size=256B | Avg RTT=0.142 ms | Throughput=22.814 Mbps
Size=512B | Avg RTT=0.158 ms | Throughput=39.673 Mbps
```

**JSON (`web/plot_results.json`):**
```json
[
  {
    "protocol": "TCP",
    "results": [
      {"payload": 8,   "avg_rtt_ms": 0.124031, "throughput_mbps": 0.843217},
      {"payload": 64,  "avg_rtt_ms": 0.131405, "throughput_mbps": 6.201384},
      {"payload": 256, "avg_rtt_ms": 0.142198, "throughput_mbps": 22.813900},
      {"payload": 512, "avg_rtt_ms": 0.158762, "throughput_mbps": 39.672541}
    ]
  }
]
```

---

## 🔐 Encryption Details

When `ENCRYPT = true`, a **deterministic XOR-shift PRNG** (seed: `1234567`) is used to generate a keystream that is XOR'd with the payload before transmission. The server applies the same operation to decrypt, since XOR is its own inverse. This is not cryptographically secure but demonstrates symmetric stream cipher principles at the application layer.

---

## 📈 Key Observations

- **TCP** provides reliable, ordered delivery with stable RTT; ideal for accuracy-critical applications
- **UDP** offers lower overhead and higher raw throughput at the cost of potential packet loss
- **Throughput scales** near-linearly with payload size in both protocols up to network limits
- **Warm-up rounds** are critical for accurate benchmarking — JIT compilation and socket buffering stabilize after initial messages

---

## 🛠️ Technologies

- **Java** — Core networking (`java.net`), I/O streams, NIO file API
- **TCP Sockets** — `Socket` / `ServerSocket` with `DataInputStream` / `DataOutputStream`
- **UDP Datagrams** — `DatagramSocket` / `DatagramPacket`
- **JSON** — Manual serialization via `PrintWriter` + `StringWriter`

---

## 📄 License

This project is open for academic and educational use. Feel free to fork, extend, and experiment.
