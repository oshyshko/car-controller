package car.controller;

import java.io.IOException;
import java.net.*;

public class Net {
    public static final String TARGET_IP = "192.168.1.15"; // TODO detect via broadcast
    public static final int TARGET_PORT = 5678;

    public static void send(State s) {
        System.out.println("Net.send: " + s);
        try {
            byte[] bytes = s.toJson().getBytes("UTF-8");

            InetAddress address = InetAddress.getByName(TARGET_IP);
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, TARGET_PORT);

            try (DatagramSocket socket = new DatagramSocket()) {
                socket.send(packet);
                socket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
