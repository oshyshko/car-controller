package shared;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import static shared.IO.println;

public abstract class Udp implements Closeable {
    public static boolean DEBUG;

    protected DatagramSocket socket;
    protected volatile boolean running;

    public Udp(int port) throws IOException {
        if (DEBUG)  println("Listening UDP packets on port " + port);

        socket = new DatagramSocket(port);
        running = true;

        final Thread t = new Thread(
                new Runnable() {
                    public void run() {
                        byte[] buffer = new byte[1024];
                        while (running) {
                            try {
                                DatagramPacket p = new DatagramPacket(buffer, buffer.length);
                                socket.receive(p);

                                InetSocketAddress from = (InetSocketAddress) p.getSocketAddress();

                                byte[] bytes = new byte[p.getLength()];
                                System.arraycopy(p.getData(), p.getOffset(), bytes, 0, p.getLength());

                                if (DEBUG) println("< " + from + " <=" + Udp.toString(bytes));

                                onReceive((InetSocketAddress) p.getSocketAddress(), bytes);
                            } catch (IOException e) {
                                if (!socket.isClosed())
                                    Errors.die(e);
                            }
                        }
                    }
                });
        t.start();
    }

    protected abstract void onReceive(InetSocketAddress from, byte[] bytes);

    public void close() throws IOException {
        running = false;
        socket.close();
    }

    public void send(InetSocketAddress to, byte[] bytes) {
        send(to, bytes, false);
    }

    public void send(InetSocketAddress to, byte[] bytes, boolean broadcast) {
        if (DEBUG) println("> " + to + " => " + Udp.toString(bytes));
        try {
            socket.send(new DatagramPacket(bytes, bytes.length, to));
        } catch (Throwable e) {
            Errors.die(e);
        }
    }

    public static String toString(byte[] bytes) {
        ArrayList<Byte> bs = new ArrayList<>();
        for (byte b : bytes)
            bs.add(b);
        return bs.toString();
    }

    public boolean isRunning() {
        return running;
    }
}
