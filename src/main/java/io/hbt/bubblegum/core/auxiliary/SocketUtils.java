package io.hbt.bubblegum.core.auxiliary;

import javax.net.ServerSocketFactory;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Random;


public class SocketUtils {

    private static final Random random = new Random(System.currentTimeMillis());


    private SocketUtils() { /* Non instantiable */ }


    /**
     * Find an available TCP port randomly selected from a range.
     * @param minPort the minimum port number.
     * @param maxPort the maximum port number.
     * @return an available TCP port number.
     * @throws IllegalStateException if no available port could be found.
     */
    public static int findAvailableTcpPort(int minPort, int maxPort) {
        return SocketType.TCP.findAvailablePort(minPort, maxPort);
    }


    /**
     * Find an available UDP port randomly selected from a range.
     * @param minPort the minimum port number.
     * @param maxPort the maximum port number.
     * @return an available UDP port number.
     * @throws IllegalStateException if no available port could be found.
     */
    public static int findAvailableUdpPort(int minPort, int maxPort) {
        return SocketType.UDP.findAvailablePort(minPort, maxPort);
    }

    
    private enum SocketType {

        TCP {
            @Override
            protected boolean isPortAvailable(int port) {
                try {
                    ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(
                        port, 1, InetAddress.getByName("localhost"));
                    serverSocket.close();
                    return true;
                }
                catch (Exception ex) {
                    return false;
                }
            }
        },

        UDP {
            @Override
            protected boolean isPortAvailable(int port) {
                try {
                    DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName("localhost"));
                    socket.close();
                    return true;
                }
                catch (Exception ex) {
                    return false;
                }
            }
        };

        /**
         * Determine if the specified port for this {@code SocketType} is
         * currently available on {@code localhost}.
         */
        protected abstract boolean isPortAvailable(int port);

        /**
         * Find a pseudo-random port number within the range
         * [{@code minPort}, {@code maxPort}].
         * @param minPort the minimum port number
         * @param maxPort the maximum port number
         * @return a random port number within the specified range
         */
        private int findRandomPort(int minPort, int maxPort) {
            int portRange = maxPort - minPort;
            return minPort + random.nextInt(portRange + 1);
        }

        /**
         * Find an available port for this {@code SocketType}, randomly selected
         * from the range [{@code minPort}, {@code maxPort}].
         * @param minPort the minimum port number
         * @param maxPort the maximum port number
         * @return an available port number for this socket type
         * @throws IllegalStateException if no available port could be found
         */
        int findAvailablePort(int minPort, int maxPort) {
            int portRange = maxPort - minPort;
            int candidatePort;
            int searchCounter = 0;
            do {
                if (searchCounter > portRange) {
                    throw new IllegalStateException(String.format(
                        "Could not find an available %s port in the range [%d, %d] after %d attempts",
                        name(), minPort, maxPort, searchCounter));
                }
                candidatePort = findRandomPort(minPort, maxPort);
                searchCounter++;
            }
            while (!isPortAvailable(candidatePort));

            return candidatePort;
        }
    }

}