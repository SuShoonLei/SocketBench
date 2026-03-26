import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;

public class UdpClient {

    private static final String HOST = "localhost";
    private static final int PORT = 6000;
    private static final int[] SIZES = {8, 64, 256, 512};
    private static final int NUM_MESSAGES = 1000;
    private static final boolean ENCRYPT = true;
    private static final int TIMEOUT_MS = 3000;

    public static void main(String[] args) throws Exception {

        InetAddress address = InetAddress.getByName(HOST);
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(TIMEOUT_MS);

        StringWriter buffer = new StringWriter();

        try (PrintWriter json = new PrintWriter(buffer)) {

            json.println("{");
            json.println("  \"protocol\": \"UDP\",");
            json.println("  \"results\": [");

            for (int s = 0; s < SIZES.length; s++) {

                int size = SIZES[s];
                byte[] payload = new byte[size];
                new Random().nextBytes(payload);

                if (ENCRYPT) {
                    xorShiftBuffer(payload, size);
                }

                byte[] recvBuffer = new byte[size];

                // -------- RTT TEST --------
                long totalRtt = 0;
                int receivedCount = 0;

                // Warm-up
                for (int i = 0; i < 100; i++) {
                    DatagramPacket warm =
                            new DatagramPacket(payload, size, address, PORT);
                    socket.send(warm);
                    try {
                        DatagramPacket resp =
                                new DatagramPacket(recvBuffer, size);
                        socket.receive(resp);
                    } catch (SocketTimeoutException ignored) {}
                }

                for (int i = 0; i < NUM_MESSAGES; i++) {

                    DatagramPacket packet =
                            new DatagramPacket(payload, size, address, PORT);

                    long start = System.nanoTime();
                    socket.send(packet);

                    DatagramPacket response =
                            new DatagramPacket(recvBuffer, size);

                    try {
                        socket.receive(response);
                        long end = System.nanoTime();
                        totalRtt += (end - start);
                        receivedCount++;
                    } catch (SocketTimeoutException ignored) {
                        // packet loss
                    }
                }

                double avgRttMs = receivedCount == 0 ? Double.NaN :
                        (totalRtt / (double) receivedCount) / 1e6;

                // -------- THROUGHPUT TEST --------
                long startThroughput = System.nanoTime();

                for (int i = 0; i < NUM_MESSAGES; i++) {
                    DatagramPacket packet =
                            new DatagramPacket(payload, size, address, PORT);
                    socket.send(packet);
                }

                int recvForThroughput = 0;
                while (recvForThroughput < NUM_MESSAGES) {
                    try {
                        DatagramPacket response =
                                new DatagramPacket(recvBuffer, size);
                        socket.receive(response);
                        recvForThroughput++;
                    } catch (SocketTimeoutException e) {
                        break;
                    }
                }

                long endThroughput = System.nanoTime();
                double seconds = (endThroughput - startThroughput) / 1e9;

                double totalBytes = (double) size * recvForThroughput * 2;
                double throughputMbps = (totalBytes * 8 / 1_000_000) / seconds;

                System.out.printf(Locale.US,
                        "UDP Size=%dB | Avg RTT=%.3f ms | Throughput=%.3f Mbps | Received=%d%n",
                        size, avgRttMs, throughputMbps, receivedCount);

                json.printf(Locale.US,
                        "    {\"payload\": %d, \"avg_rtt_ms\": %.6f, \"throughput_mbps\": %.6f, \"received\": %d}",
                        size, avgRttMs, throughputMbps, receivedCount);

                if (s < SIZES.length - 1) json.println(",");
                else json.println();
            }

            json.println("  ]");
            json.println("}");

            json.flush();
            String runJson = buffer.toString().trim();
            appendRunJson("web/plot_results.json", runJson);
        }

        socket.close();
    }

    private static void appendRunJson(String filePath, String runJson) throws IOException {
        Path path = Path.of(filePath);
        String newContent;

        if (Files.exists(path)) {
            String existing = Files.readString(path, StandardCharsets.UTF_8).trim();

            if (existing.isEmpty()) {
                newContent = "[\n" + runJson + "\n]\n";
            } else if (existing.startsWith("[")) {
                int lastBracket = existing.lastIndexOf(']');
                if (lastBracket <= 0) {
                    newContent = "[\n" + runJson + "\n]\n";
                } else {
                    String before = existing.substring(0, lastBracket).trim();
                    if (before.endsWith("[")) {
                        newContent = "[\n" + runJson + "\n]\n";
                    } else {
                        newContent = before + ",\n" + runJson + "\n]\n";
                    }
                }
            } else {
                newContent = "[\n" + existing + ",\n" + runJson + "\n]\n";
            }
        } else {
            newContent = "[\n" + runJson + "\n]\n";
        }

        Files.writeString(path, newContent, StandardCharsets.UTF_8);
    }

    private static void xorShiftBuffer(byte[] data, int length) {
        long seed = 1234567L;
        for (int i = 0; i < length; i++) {
            seed = xorShift(seed);
            data[i] ^= (byte) seed;
        }
    }

    private static long xorShift(long r) {
        r ^= r << 13;
        r ^= r >>> 7;
        r ^= r << 17;
        return r;
    }
}