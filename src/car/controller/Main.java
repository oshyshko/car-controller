package car.controller;

import shared.Network;
import shared.Udp;

import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws Exception {
        Udp.DEBUG = true;

        Network network = new Network();

        byte id = 0;
        while (true) {
            network.controller_update(
                    new InetSocketAddress(Network.CAR_IP, Network.PORT),
                    id++, (byte) (id + 10), (byte) (id + 20));

            // TODO adjust sleep (period - delta-from-previous-call)
            Thread.sleep(100);
        }
    }
}
