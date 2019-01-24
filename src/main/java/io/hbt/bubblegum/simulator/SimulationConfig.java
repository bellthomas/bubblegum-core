package io.hbt.bubblegum.simulator;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import io.hbt.bubblegum.core.auxiliary.NetworkingHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

public class SimulationConfig {
    private boolean loadedSuccessfully = false;

    // Simulation Details

    // Bootstrap
    boolean doingBootstrap = false;
    InetAddress addr;
    int port;
    String recipient;

    // Background
    int numNetworks = 1;
    int numNetworkNodes = 1;
    int newPostsEveryHour = 0;
    int feedRetrievalsEveryHour = 60;
    int initialiseWithFixedPostNumber = 60;

    public SimulationConfig(String file) {
        this.loadedSuccessfully = false;
        try {
            // Read and parse YAML.
            File f = new File(file);
            YamlReader reader = new YamlReader(new FileReader(f));
            Object object = reader.read();

            // Build Map of data and pass to Configuration.
            Map map = (Map) object;

            if(map.containsKey("background")) {
                Map background = (Map) map.get("background");

                if(background.containsKey("networks")) {
                    Integer networks = this.toInt((String) background.get("networks"));
                    if(networks != null) this.numNetworks = networks;
                }

                if(background.containsKey("nodesPerNetwork")) {
                    Integer nodesPerNetwork = this.toInt((String) background.get("nodesPerNetwork"));
                    if(nodesPerNetwork != null) this.numNetworkNodes = nodesPerNetwork;
                }

                if(background.containsKey("newPostsEveryHour")) {
                    Integer newPostsEveryHour = this.toInt((String) background.get("newPostsEveryHour"));
                    if(newPostsEveryHour != null) this.newPostsEveryHour = newPostsEveryHour;
                }

                if(background.containsKey("feedRetrievalsEveryHour")) {
                    Integer feedRetrievalsEveryHour = this.toInt((String) background.get("feedRetrievalsEveryHour"));
                    if(feedRetrievalsEveryHour != null) this.feedRetrievalsEveryHour = feedRetrievalsEveryHour;
                }

                if(background.containsKey("initialiseWithFixedPostNumber")) {
                    Integer initialiseWithFixedPostNumber = this.toInt((String) background.get("initialiseWithFixedPostNumber"));
                    if(initialiseWithFixedPostNumber != null) this.initialiseWithFixedPostNumber = initialiseWithFixedPostNumber;
                }

            }


            this.loadedSuccessfully = true;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (YamlException e) {
            e.printStackTrace();
        }
    }

    protected void runningBootstrap(String address, String port, String recipient) {
        System.out.println("[SimulationConfig] Bootstrapping network to " + address + ":" + port + " ~ " + recipient);
        try {
            InetAddress addr = NetworkingHelper.getInetAddress(address);
            Integer p = this.toInt(port);
            if(p != null) {
                this.addr = addr;
                this.port = p;
                this.recipient = recipient;
                this.doingBootstrap = true;
            }
            else {
                System.out.println("[SimulationConfig] Failed to parse port, ignoring bootstrap");
            }
        } catch (UnknownHostException e) {
            System.out.println("[SimulationConfig] Failed to parse address, ignoring bootstrap");
        }
    }

    public int getNumNetworks() {
        if(this.doingBootstrap) return 1;
        else return this.numNetworks;
    }

    public int getNumNetworkNodes() {
        return (this.numNetworkNodes < 1) ? 1 : this.numNetworkNodes;
    }

    public int getNewPostsEveryHour() {
        return (this.newPostsEveryHour < 0) ? 0 : this.newPostsEveryHour;
    }

    public int getFeedRetrievalsEveryHour() {
        return (this.feedRetrievalsEveryHour < 0) ? 0 : this.feedRetrievalsEveryHour;
    }

    public int getInitialiseWithFixedPostNumber() {
        return (this.initialiseWithFixedPostNumber < 0) ? 0 : this.initialiseWithFixedPostNumber;
    }

    public InetAddress getAddr() {
        return addr;
    }

    public int getPort() {
        return port;
    }

    public String getRecipient() {
        return recipient;
    }

    public boolean isDoingBootstrap() {
        return doingBootstrap;
    }

    public Integer toInt(String s) {
        try {
            return Integer.valueOf(s);
        }
        catch (NumberFormatException nfe) {
            return null;
        }
    }
}
