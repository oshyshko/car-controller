package car.controller;

import shared.Errors;
import shared.Net;
import shared.Sleeper;
import shared.Udp;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import static shared.IO.println;

public class Main {
    public static void main(String[] args) throws Exception {
        Udp.DEBUG = false;

        Map<InetAddress, InetAddress> addresses2broadcasts = Udp.address2broadcast();
        println("My addresses + broadcasts are: " + addresses2broadcasts);
        println();

        if (addresses2broadcasts.size() != 1)
            Errors.die("addresses2broadcasts expected to have exactly one pair, buts was: " +
                    addresses2broadcasts.size() + " (TODO implement selection)");

        InetAddress self      = addresses2broadcasts.keySet().iterator().next();
        InetAddress broadcast = addresses2broadcasts.values().iterator().next();

        Net net = new Net(Net.KIND_CONTROLLER, self, broadcast) {
            protected void all_onShout(InetAddress from) {
                // do nothing
            }

            protected void controller_onUpdateConfirm(InetAddress from, byte id, long ping) {
                println("~ " + ping);
            }
        };

        // TODO make this calls from actual UI

        byte id = 0;
        Sleeper s = new Sleeper();
        while (true) {
            List<InetAddress> cars = net.peers(Net.KIND_CAR);

            if (!cars.isEmpty()) {
                net.controller_update(
                        cars.get(0),
                        id++, (byte) (id + 10), (byte) (id + 20));
            }

            s.sleep(1000); // TODO change to 100 for testing
        }

    }
}
