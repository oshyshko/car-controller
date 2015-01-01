package car.controller;

import shared.Net;
import shared.Sleeper;
import shared.Udp;

import java.net.InetSocketAddress;

public class Main {
    public static final String CAR_IP = "192.168.88.82";
    public static final String BROADCAST_IP = "192.168.88.255"; // TODO get via API

    public static void main(String[] args) throws Exception {
        Udp.DEBUG = true;

        Net net = new Net("controller", BROADCAST_IP);

        byte id = 0;
        Sleeper s = new Sleeper();
        while (true) {
            net.controller_update(
                    new InetSocketAddress(CAR_IP, Net.PORT),
                    id++, (byte) (id + 10), (byte) (id + 20));

            s.sleep(1000); // TODO change to 100 for testing
        }

    }
}
