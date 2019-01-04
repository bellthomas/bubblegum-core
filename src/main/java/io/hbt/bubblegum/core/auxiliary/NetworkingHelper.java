package io.hbt.bubblegum.core.auxiliary;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NetworkingHelper {

    private static final Set<InetAddress> inetAddressRepository = new HashSet<>();

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


    public static InetAddress getInetAddress(InetAddress addr) {
        for(InetAddress repoAddr : NetworkingHelper.inetAddressRepository) {
            if(repoAddr.equals(addr)) return repoAddr;
        }
        return addr;
    }

    public static InetAddress getInetAddress(String hostname) throws UnknownHostException {
        InetAddress actual = InetAddress.getByName(hostname);
        return NetworkingHelper.getInetAddress(actual);
    }

    public static InetAddress getLocalInetAddress() {
        try {
            InetAddress actual = InetAddress.getLocalHost();
            return NetworkingHelper.getInetAddress(actual);

        } catch (UnknownHostException e) {
            return null;
        }
    }

    public static void main(String[] args) {
        try {
            InetAddress local = InetAddress.getByName("192.168.0.1");
            inetAddressRepository.add(local);
            System.out.println(local.getHostName());

            InetAddress local2 = getInetAddress("192.168.0.1");
            InetAddress local3 = getInetAddress("192.168.0.1");
            InetAddress local4 = getInetAddress("192.168.0.1");

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
