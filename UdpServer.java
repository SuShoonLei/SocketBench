import java.net.*;

public class UdpServer {

    private static final boolean ENCRYPT = true;
    private static final int MAX_BUFFER = 2048;

    public static void main(String[] args) throws Exception {

        int port = 6000;
        DatagramSocket socket = new DatagramSocket(port);

        System.out.println("UDP Server listening on port " + port);

        byte[] buffer = new byte[MAX_BUFFER];

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            int len = packet.getLength();

            if (ENCRYPT) {
                xorShiftBuffer(buffer, len);
            }

            DatagramPacket response =
                    new DatagramPacket(buffer, len,
                            packet.getAddress(), packet.getPort());

            socket.send(response);
        }
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