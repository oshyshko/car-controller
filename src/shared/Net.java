package shared;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static shared.IO.println;

public class Net implements Closeable {
    public static final int PORT = 46352;

    public static final int MS_BETWEEN_SHOUTS = 1000;

    // kinds (used in shouting)
    public static final byte KIND_CAR = 1;
    public static final byte KIND_CONTROLLER = 2;


    // function ids
    public static final int SHOUT = 0;
    public static final int CAR_UPDATE = 2;
    public static final int CONTROLLER_UPDATE_CONFIRM = 1;

    private final InetAddress self;
    private final InetAddress broadcast;

    private final Udp udp;

    private final Cache<InetAddress, Byte> peer2kind =
            CacheBuilder.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(10, TimeUnit.SECONDS)
                    .build();


    private final Cache<Pair<InetAddress, Byte>, Long> peer_packetId2millis =
            CacheBuilder.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(10, TimeUnit.SECONDS)
                    .build();

    public Net(final byte kind, final InetAddress self, InetAddress broadcast) throws IOException {
        this.self = self;
        this.broadcast = broadcast;

        udp = new Udp(PORT) {
            protected void onReceive(InetSocketAddress fromSocket, byte[] bytes) {
                InetAddress from = fromSocket.getAddress();
                if (bytes.length == 0) {
                    println("! Got empty packet");
                } else if (fromSocket.getPort() != PORT) {
                    println("! Expected a packet on port " + PORT + " bot got on port " + fromSocket.getPort());
                } else {
                    switch (bytes[0]) {
                        case SHOUT:
                            byte kind = bytes[1];

                            // shout -- the only pre-implemented function
                            //if (!self.equals(from.getAddress()))
                            peer2kind.put(from, kind);

                            // but you can still override if you need to get shout callbacks
                            all_onShout(from);

                            break;

                        case CAR_UPDATE:
                            if (bytes.length != 4) {
                                println("! Got invalid packet: " + Udp.toString(bytes));
                            } else {
                                byte id = bytes[1];
                                byte steering = bytes[2];
                                byte speed = bytes[3];

                                car_onUpdate(from, id, steering, speed);
                            }
                            break;

                        case CONTROLLER_UPDATE_CONFIRM:
                            if (bytes.length != 2) {
                                println("! Got invalid packet: " + Udp.toString(bytes));
                            } else {
                                byte id = bytes[1];

                                // TODO count missing packets?
                                // TODO remove timed-out ids by checking if time delta is insane

                                long now = System.currentTimeMillis();
                                Long then = peer_packetId2millis.getIfPresent(new Pair<>(from, id));


                                long ping = then == null
                                        ? -1
                                        : now - then;

                                if (then == null) {
                                    println("! Got pong from " + from + " with unknown id: " + id);
                                }

                                controller_onUpdateConfirm(from, id, ping);
                            }
                            break;

                        default:
                            println("! Got unknown packet: " + Udp.toString(bytes));

                            all_onUnknown(from, bytes);
                            break;

                    }
                }
            }
        };

        // shouter
        try {
            new Thread() {
                public void run() {
                    byte[] shoutBytes = new byte[] {SHOUT, kind};

                    Sleeper s = new Sleeper();
                    while (udp.isRunning()) {
                        all_shout(shoutBytes);
                        s.sleep(MS_BETWEEN_SHOUTS);
                    }
                }
            }.start();
        } catch (Throwable t) {
            udp.close();
            throw Errors.die(t);
        }
    }

    public void close() throws IOException {
        udp.close();
    }

    // Querying
    public Map<InetAddress, Byte> peer2kind() {
        return peer2kind.asMap();
    }
    public boolean isSelf(InetAddress address) {
        return self.equals(address);
    }

    // Callbacks (override them)
    protected void all_onShout(InetAddress from) { /* do nothing */ }
    protected void all_onUnknown(InetAddress from, byte[] bytes) { /* do nothing */ }

    protected void car_onUpdate(InetAddress from, byte id, byte steering, byte speed) { /* do nothing */ }

    protected void controller_onUpdateConfirm(InetAddress from, byte id, long ping) { /* do nothing */ }


    // Sending -- used by controller
    /**
     * @param to
     * @param id       Id sequence generated by controller.
     *                 The car should reply with confirmation packed containing this id.
     *                 Used for detection of latency and missing packets.
     * @param steering -127 = left,     0 = central, 127 = right
     * @param speed    -127 = backward, 0 = neutral, 127 = forward
     */
    public void controller_update(InetAddress to, byte id, byte steering, byte speed) {
        peer_packetId2millis.put(
                new Pair<>(to, id),
                System.currentTimeMillis());

        udp.send(withPort(to), new byte[]{CAR_UPDATE, id, steering, speed});
    }

    // Sending -- used by car
    public void car_updateReceived(InetAddress to, byte id) {
        udp.send(withPort(to), new byte[]{CONTROLLER_UPDATE_CONFIRM, id});
    }

    // Sending -- used by car + controller
    private void all_shout(byte[] shoutBytes) {
        udp.send(withPort(broadcast), shoutBytes, true);
    }

    private InetSocketAddress withPort(InetAddress a) {
        return new InetSocketAddress(a, PORT);
    }

    public List<InetAddress> peers(byte kindMask) {
        List<InetAddress> res = new ArrayList<>();
        for (Map.Entry<InetAddress, Byte> kv : peer2kind().entrySet())
            if ((kv.getValue() & kindMask) > 0)
                res.add(kv.getKey());
        return res;
    }


    // various garbage and parts missing in Java
    public static class Pair<A, B> {
        private final A a;
        private final B b;

        public Pair(A a, B b) {
            this.a =a;
            this.b = b;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Pair pair = (Pair) o;

            if (a != null ? !a.equals(pair.a) : pair.a != null) return false;
            if (b != null ? !b.equals(pair.b) : pair.b != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = a != null ? a.hashCode() : 0;
            result = 31 * result + (b != null ? b.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Pair{" +
                    "a=" + a +
                    ", b=" + b +
                    '}';
        }
    }
}
