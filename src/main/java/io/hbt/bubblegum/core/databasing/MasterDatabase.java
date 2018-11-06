package io.hbt.bubblegum.core.databasing;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MasterDatabase {

    private static final String MASTER_DB_NAME = "_master.db";
    private static MasterDatabase instance;

    private MasterDatabase() { /* Singleton */ }

    protected synchronized static MasterDatabase getInstance() {
        if (MasterDatabase.instance == null) MasterDatabase.instance = new MasterDatabase();
        return MasterDatabase.instance;
    }

    public void updateNetwork(BubblegumNode network) {
        this.updateNetworks(new ArrayList<>() {{ add(network); }});
    }

    public synchronized void updateNetworks(List<BubblegumNode> networks) {

        Connection connection = null;
        try {
            this.checkDatabasesDirectory();

            // create a databasing connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + Database.DB_FOLDER_PATH + MASTER_DB_NAME);
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.

            statement.executeUpdate(this.setupMasterTableSQL());

            for(BubblegumNode network : networks) {
                ResultSet rs = statement.executeQuery(this.rowExistencecheckSQL(network));
                if(rs.next() && rs.getInt(1) == 1) {
                    // The network already exists, update
                    int result = statement.executeUpdate(this.updateNetworkDetailsSQL(network));
                    if(result == 1) {
                        // success
                    }
                }
                else {
                    // Network is new, create new entry
                    int result = statement.executeUpdate(this.insertNewNetworkSQL(network));
                    if(result == 1) {
                        // success
                    }
                }
            }

        } catch (SQLException e) {
            // if the error message is "out of memory",
            // it probably means no databasing file is found
            System.err.println(e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if(connection != null) connection.close();
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e);
                e.printStackTrace();
            }
        }

    }

    public Map<Integer, List<NetworkDetails>> loadNetworksFromDatabase() {
        Connection connection = null;
        try {
            this.checkDatabasesDirectory();

            // create a databasing connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + Database.DB_FOLDER_PATH + MASTER_DB_NAME);
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.

            statement.executeUpdate(this.setupMasterTableSQL());

            Map<Integer, List<NetworkDetails>> details = new HashMap<>();
            ResultSet rs = statement.executeQuery(this.getAllNetworksSQL());
            while (rs.next()) {
                String id = rs.getString(1);
                String network = rs.getString(2);
                String hash = rs.getString(3);
                int port = rs.getInt(4);
                if(!details.containsKey(port)) details.put(port, new ArrayList<>());
                details.get(port).add(new NetworkDetails(id, network, hash, port));
            }

            return details;

        } catch (SQLException e) {
            // if the error message is "out of memory",
            // it probably means no databasing file is found
            System.err.println(e.getMessage());
        } finally {
            try {
                if(connection != null) connection.close();
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e);
            }
        }

        return new HashMap<>();
    }

    protected void checkDatabasesDirectory() {
        File directory = new File(Database.DB_FOLDER_PATH);
        if (!directory.exists()) directory.mkdir();
    }

    public void resetDatabases() {
        File directory = new File(Database.DB_FOLDER_PATH);
        if(directory.exists()) {
            this.delete(directory);
        }
    }

    private void delete(File f) {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) delete(c);
        }
        f.delete();
    }

    private String setupMasterTableSQL() {
        /* create table if not exists networks( id varchar(255) NOT NULL, hash varchar(255) NOT NULL, port int NOT NULL, CONSTRAINT key PRIMARY KEY (id,hash) ) */
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS networks( ");
        sb.append("id VARCHAR(255) NOT NULL, ");
        sb.append("network VARCHAR(255) NOT NULL, ");
        sb.append("hash VARCHAR(255) NOT NULL, ");
        sb.append("port int NOT NULL, ");
        sb.append("PRIMARY KEY (id) ");
        sb.append(")");
        return sb.toString();
    }

    private String rowExistencecheckSQL(BubblegumNode network) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT EXISTS( ");
        sb.append("SELECT 1 FROM networks WHERE ");
        sb.append("id='"+ network.getIdentifier() +"' LIMIT 1 ");
        sb.append(")");
        return sb.toString();
    }

    private String updateNetworkDetailsSQL(BubblegumNode network) {
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE networks ");
        sb.append("SET network='"+ network.getNetworkIdentifier() +"', hash='"+ network.getNodeIdentifier() +"', port="+ network.getServer().getPort() +" ");
        sb.append("WHERE id='"+ network.getIdentifier() +"'");
        return sb.toString();
    }

    private String insertNewNetworkSQL(BubblegumNode network) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO networks ");
        sb.append("(id, network, hash, port) ");
        sb.append("VALUES ('"+ network.getIdentifier()+"', '"+ network.getNetworkIdentifier() +"', '"+ network.getNodeIdentifier() +"', "+ network.getServer().getPort() +")");
        return sb.toString();
    }

    private String getAllNetworksSQL() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM networks");
        return sb.toString();
    }


}
