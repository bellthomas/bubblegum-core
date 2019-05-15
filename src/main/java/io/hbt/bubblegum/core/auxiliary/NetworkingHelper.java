package io.hbt.bubblegum.core.auxiliary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;


/**
 * Helper methods for Networking.
 */
public class NetworkingHelper {

    private static final Set<InetAddress> inetAddressRepository = new HashSet<>();
    private static boolean lookupExternalIP = true;
    private static InetAddress proxy;
    private static String externalIP;

    /**
     * Constructor.
     * non-instantiable.
     */
    private NetworkingHelper() { }

    /**
     * Simple check for a valid port range.
     * @param port Port to check.
     * @return Whether it is valid.
     */
    public static boolean validPort(int port) {
        return (port > -1 && port < 65536);
    }

    /**
     * Set whether to lookup the machine's external IP address.
     * @param external
     */
    public static void setLookupExternalIP(boolean external) {
        NetworkingHelper.lookupExternalIP = external;
    }

    /**
     * Set whether the instance is using a proxy.
     * @param address The proxy's address.
     */
    public static void setProxy(InetAddress address) { NetworkingHelper.proxy = address; }


    /**
     * Retrieve a cached instance of an InetAddress instance if found.
     * The eases some memory churn.
     * @param addr The address to find.
     * @return The address to use.
     */
    public static InetAddress getInetAddress(InetAddress addr) {
        for(InetAddress repoAddr : NetworkingHelper.inetAddressRepository) {
            if(repoAddr.equals(addr)) return repoAddr;
        }
        return addr;
    }

    /**
     * etrieve a cached instance of an InetAddress instance if found.
     * @param hostname The string hostname to get.
     * @return The address to use.
     * @throws UnknownHostException
     */
    public static InetAddress getInetAddress(String hostname) throws UnknownHostException {
        InetAddress actual = InetAddress.getByName(hostname);
        return NetworkingHelper.getInetAddress(actual);
    }

    /**
     * Get the machine's local address.
     * Tweaked depending on proxy/external mode settings.
     * @return The local InetAddress to use.
     */
    public static InetAddress getLocalInetAddress() {
        try {
            // Proxy mode, so declare self as the proxy's address.
            if(NetworkingHelper.proxy != null) return NetworkingHelper.proxy;

            // Using a native address, check for localhost mode.
            String externalIP = lookupExternalIP ? NetworkingHelper.getExternalIP() : null;
            if(externalIP == null) return NetworkingHelper.getInetAddress(InetAddress.getLocalHost());
            else return NetworkingHelper.getInetAddress(externalIP);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /**
     * Force external IP lookup if required.
     * @return The machine's external IP.
     */
    public static InetAddress getProxyLocalAddress() {
        try {
            String externalIP = NetworkingHelper.getExternalIP();
            if(externalIP == null) return NetworkingHelper.getInetAddress(InetAddress.getLocalHost());
            else return NetworkingHelper.getInetAddress(externalIP);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /**
     * Fetch the external IP if not previously checked.
     * @return The external IP.
     */
    private static String getExternalIP() {
        if(NetworkingHelper.externalIP == null) NetworkingHelper.externalIP = NetworkingHelper.lookupExternalIP();
        return NetworkingHelper.externalIP;
    }

    /**
     * Lookup the machine's external IP using the Amazon Check IP tool.
     * @return The machine's external IP.
     */
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

} // end NetworkingHelper class
