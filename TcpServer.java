import java.io.*;
import java.net.*;

public class TcpServer {

    private static final boolean ENCRYPT = true;

    public static void main(String[] args) throws Exception {
        int port = 5020;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("TCP Server listening on port " + port);

        while (true) {
            Socket socket = serverSocket.accept();
            handleClient(socket);
        }
    }

    private static void handleClient(Socket socket) {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(socket.getInputStream()));
             DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(socket.getOutputStream()))) {

            socket.setTcpNoDelay(true);

            while (true) {
                int size;
                try {
                    size = in.readInt();
                } catch (EOFException e) {
                    break;
                }

                byte[] buffer = new byte[size];
                in.readFully(buffer);

                if (ENCRYPT) {
                    xorShiftBuffer(buffer);
                }

                out.writeInt(size);
                out.write(buffer);
                out.flush();
            }

        } catch (IOException ignored) {
        }
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