package car.controller;

import shared.Network;
import shared.Udp;

import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws Exception {
        Udp.DEBUG = false;

        Network.Controller.start();

        byte id = 0;
        while (true) {
            Network.Controller.update(
                    new InetSocketAddress(Network.CAR_IP, Network.CAR_PORT),
                    id++, (byte) (id + 10), (byte) (id + 20));

            // TODO adjust sleep (period - delta-from-previous-call)
            Thread.sleep(100);
        }
    }
}
