package shared;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static shared.IO.println;

public class Udp {
    public static boolean DEBUG = false;

    public static void send(InetSocketAddress to, byte[] bytes) {
        if (DEBUG) println("> " + to + " => " + toString(bytes));
        try {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.send(new DatagramPacket(bytes, bytes.length, to));
                socket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Server listen(int port, final OnReceive onReceive) throws IOException {
        if (DEBUG)  println("Listening UDP packets on port " + port);
        final DatagramSocket ss = new DatagramSocket(port);
        final AtomicBoolean running = new AtomicBoolean(true);

        final Thread t = new Thread(
                new Runnable() {
                    public void run() {
                        byte[] buffer = new byte[1024];
                        while (running.get()) {
                            try {
                                DatagramPacket p = new DatagramPacket(buffer, buffer.length);
                                ss.receive(p);

                                InetSocketAddress from = (InetSocketAddress) p.getSocketAddress();

                                byte[] bytes = new byte[p.getLength()];
                                System.arraycopy(p.getData(), p.getOffset(), bytes, 0, p.getLength());

                                if (DEBUG) println("< " + from + " <=" + Udp.toString(bytes));

                                onReceive.onReceive(from, bytes);
                            } catch (IOException e) {
                                if (!ss.isClosed())
                                    Errors.die(e);
                            }
                        }
                    }
                });
        t.start();

        return new Server() {
            public void close() throws IOException {
                running.set(false);
                ss.close();
            }
        };
    }

    public static String toString(byte[] bytes) {
        ArrayList<Byte> bs = new ArrayList<>();
        for (byte b : bytes)
            bs.add(b);
        return bs.toString();
    }


    public static interface Server extends Closeable {}
    public static interface OnReceive { void onReceive(InetSocketAddress from, byte[] bytes); }
}
