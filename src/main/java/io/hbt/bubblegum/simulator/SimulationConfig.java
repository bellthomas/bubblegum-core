package io.hbt.bubblegum.simulator;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map;

public class SimulationConfig {
    private boolean loadedSuccessfully = false;

    // Simulation Details

    // Background
    int numNetworks = 1;
    int numNetworkNodes = 1;
    int newPostsEveryHour = 0;
    int feedRetrievalsEveryHour = 60;

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

            }


            this.loadedSuccessfully = true;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (YamlException e) {
            e.printStackTrace();
        }
    }


    public int getNumNetworks() {
        return this.numNetworks;
    }

    public int getNumNetworkNodes() {
        return this.numNetworkNodes;
    }

    public int getNewPostsEveryHour() {
        return newPostsEveryHour;
    }

    public int getFeedRetrievalsEveryHour() {
        return feedRetrievalsEveryHour;
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
