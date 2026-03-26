import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;

public class TcpClient {

    private static final String HOST = "localhost";
    private static final int PORT = 5020;
    private static final int[] SIZES = {8, 64, 256, 512};
    private static final int NUM_MESSAGES = 1000;
    private static final boolean ENCRYPT = true;

    public static void main(String[] args) throws Exception {

        StringWriter buffer = new StringWriter();

        try (Socket socket = new Socket(HOST, PORT);
             DataInputStream in = new DataInputStream(
                     new BufferedInputStream(socket.getInputStream()));
             DataOutputStream out = new DataOutputStream(
                     new BufferedOutputStream(socket.getOutputStream()));
             PrintWriter json = new PrintWriter(buffer)) {

            socket.setTcpNoDelay(true);

            json.println("{");
            json.println("  \"protocol\": \"TCP\",");
            json.println("  \"results\": [");

            for (int s = 0; s < SIZES.length; s++) {

                int size = SIZES[s];
                byte[] payload = new byte[size];
                new Random().nextBytes(payload);

                if (ENCRYPT) {
                    xorShiftBuffer(payload);
                }

                // -------- RTT TEST --------
                long totalRtt = 0;

                // Warmup
                for (int i = 0; i < 100; i++) {
                    out.writeInt(size);
                    out.write(payload);
                    out.flush();
                    in.readInt();
                    in.readFully(new byte[size]);
                }

                for (int i = 0; i < NUM_MESSAGES; i++) {
                    long start = System.nanoTime();

                    out.writeInt(size);
                    out.write(payload);
                    out.flush();

                    in.readInt();
                    in.readFully(new byte[size]);

                    long end = System.nanoTime();
                    totalRtt += (end - start);
                }

                double avgRttMs = (totalRtt / (double) NUM_MESSAGES) / 1e6;

                // -------- THROUGHPUT TEST --------
                long startThroughput = System.nanoTime();

                for (int i = 0; i < NUM_MESSAGES; i++) {
                    out.writeInt(size);
                    out.write(payload);
                }
                out.flush();

                for (int i = 0; i < NUM_MESSAGES; i++) {
                    in.readInt();
                    in.readFully(new byte[size]);
                }

                long endThroughput = System.nanoTime();
                double seconds = (endThroughput - startThroughput) / 1e9;

                double totalBytes = (double) size * NUM_MESSAGES * 2;
                double throughputMbps = (totalBytes * 8 / 1_000_000) / seconds;

                System.out.printf(Locale.US,
                        "Size=%dB | Avg RTT=%.3f ms | Throughput=%.3f Mbps%n",
                        size, avgRttMs, throughputMbps);

                json.printf(Locale.US,
                        "    {\"payload\": %d, \"avg_rtt_ms\": %.6f, \"throughput_mbps\": %.6f}",
                        size, avgRttMs, throughputMbps);

                if (s < SIZES.length - 1) json.println(",");
                else json.println();
            }

            json.println("  ]");
            json.println("}");

            json.flush();
            String runJson = buffer.toString().trim();
            appendRunJson("web/plot_results.json", runJson);
        }
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

    private static void xorShiftBuffer(byte[] data) {
        long seed = 1234567L;
        for (int i = 0; i < data.length; i++) {
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