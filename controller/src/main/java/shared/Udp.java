package shared;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.*;

import static shared.IO.println;

public abstract class Udp implements Closeable {
    public static boolean DEBUG;

    protected DatagramSocket socket;
    protected volatile boolean running;

    public Udp(int port) throws IOException {
        socket = new DatagramSocket(port);
        socket.setBroadcast(true);
        running = true;

        if (DEBUG)  println("Listening for UDP packets on " + socket.getLocalSocketAddress());

        Threads.fork(new Runnable() {
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
    }

    protected abstract void onReceive(InetSocketAddress from, byte[] bytes);

    public void close() throws IOException {
        running = false;
        socket.close();
    }

    public void send(InetSocketAddress to, byte[] bytes) {
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

    // util
    public static Map<InetAddress, InetAddress> address2broadcast() {
        try {
            Map<InetAddress, InetAddress> address2broadcast = new HashMap<>();
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces()))
                if (ni != null && !ni.isLoopback() && ni.isUp())
                    for (InterfaceAddress address : ni.getInterfaceAddresses())
                        if (address != null && address.getBroadcast() != null)
                            address2broadcast.put(address.getAddress(), address.getBroadcast());
            return address2broadcast;
        } catch (SocketException e) {
            return Collections.EMPTY_MAP;
        }
    }
}
