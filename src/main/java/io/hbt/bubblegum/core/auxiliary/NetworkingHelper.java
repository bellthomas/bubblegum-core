package io.hbt.bubblegum.core.auxiliary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NetworkingHelper {

    private static final Set<InetAddress> inetAddressRepository = new HashSet<>();
    private static boolean lookupExternalIP = true;

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
            String externalIP = lookupExternalIP ? NetworkingHelper.lookupExternalIP() : null;
            if(externalIP == null) return NetworkingHelper.getInetAddress(InetAddress.getLocalHost());
            else return NetworkingHelper.getInetAddress(externalIP);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public static void setLookupExternalIP(boolean external) {
        NetworkingHelper.lookupExternalIP = external;
    }

    private static String lookupExternalIP() {
        BufferedReader in = null;
        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            in = new BufferedReader(new InputStreamReader(
                whatismyip.openStream()));
            String ip = in.readLine();
            return ip;
        } catch (MalformedURLException e) {
            return null;
        } catch (IOException e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    in = null;
                }
            }
        }
    }
}
