package io.hbt.bubblegum.core.auxiliary;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetworkingHelper {
    private NetworkingHelper() { }

    public static boolean validPort(int port) {
        return (port > -1 && port < 65536);
    }

    public static InetAddress nameToInetAddress(String host) {
        try {
            return InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
