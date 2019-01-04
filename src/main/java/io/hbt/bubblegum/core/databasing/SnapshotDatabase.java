package io.hbt.bubblegum.core.databasing;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;
import io.hbt.bubblegum.core.kademlia.router.RouterBucket;
import io.hbt.bubblegum.core.kademlia.router.RouterNode;
import io.hbt.bubblegum.core.kademlia.router.RoutingTable;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SnapshotDatabase {

    protected static final String DB_FOLDER_PATH = Database.DB_FOLDER_PATH + "snapshots/";

    protected SnapshotDatabase() { /* Static only */ }

    public static boolean saveRouterSnapshot(BubblegumNode localNode, RoutingTable routingTable) {

        String identifier = localNode.getIdentifier();
        Connection connection = null;
        try {
            checkDatabasesDirectory();

            // create a databasing connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FOLDER_PATH + identifier + ".db");
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.

            statement.executeUpdate(setupSnapshotTableSQL());
            statement.executeUpdate(truncateOldSnapshotSQL());

            int maxBucket = routingTable.getGreatestNonEmptyBucket();
            connection.setAutoCommit(false);
            for(int i = 0; i <= maxBucket; i++) {
                RouterBucket bucket = routingTable.getBucket(i);
                if(bucket != null) {
                    String insertSQL = insertRouterNodesInBucketSQL(bucket);
                    if(insertSQL.length() > 0) statement.addBatch(insertSQL);
                }
            }

            statement.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);

//            System.out.println("Saved snapshot for " + localNode.getIdentifier());

            return true;

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
            }
        }
        return false;
    }

    public static Map<Integer, List<Set<RouterNode>>> buildRoutingTableNodesFromSnapshot(String identifier) {
        File snapshotFile = new File(DB_FOLDER_PATH + identifier + ".db");
        if(!snapshotFile.exists()) return null;

//        System.out.println("Building from snapshot: " + identifier);

        // Initialise return structure
        HashMap<Integer, List<Set<RouterNode>>> result = new HashMap<>();
        for(int i = 0; i < NodeID.KEY_BIT_LENGTH; i++) {
            result.put(i, new ArrayList<Set<RouterNode>>() {{ add(new HashSet<>()); add(new HashSet<>()); }});
        }

        Connection connection = null;
        try {
            // create a databasing connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FOLDER_PATH + identifier + ".db");
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.

            statement.executeUpdate(setupSnapshotTableSQL());
            ResultSet rs = statement.executeQuery(getAllSnapshotNodes());
            while(rs.next()) {
                // read the result set
//                System.out.println("node = " + rs.getString(1));
//                System.out.println("address = " + rs.getString(2));
//                System.out.println("port = " + rs.getInt(3));
//                System.out.println("failedResponses = " + rs.getInt(5));
//                System.out.println("bucket = " + rs.getInt(6));
//                System.out.println("latestResponse = " + rs.getLong(4));

                int bucket = rs.getInt(6);
                int replacement = rs.getInt(7);
                RouterNode node = RouterNode.buildFromSnapshotNode(
                    rs.getString(1),
                    rs.getString(2),
                    rs.getInt(3),
                    rs.getLong(4),
                    rs.getInt(5)
                );

                if(node != null && bucket >= 0 && bucket < NodeID.KEY_BIT_LENGTH && (replacement == 0 || replacement == 1)) {
                    result.get(bucket).get(replacement).add(node);
                }
            }

            return result;

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if(connection != null) connection.close();
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e);
            }
        }

        return null;
    }

    protected static void checkDatabasesDirectory() {
        File directory = new File(DB_FOLDER_PATH);
        if (!directory.exists()) directory.mkdir();
    }


    private static String setupSnapshotTableSQL() {
        /* create table if not exists networks( id varchar(255) NOT NULL, hash varchar(255) NOT NULL, port int NOT NULL, CONSTRAINT key PRIMARY KEY (id,hash) ) */
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS snapshot( ");
        sb.append("node VARCHAR(255) NOT NULL, ");
        sb.append("address VARCHAR(255) NOT NULL, ");
        sb.append("port int NOT NULL, ");
        sb.append("latestResponse INTEGER NOT NULL, ");
        sb.append("failedResponses int NOT NULL, ");
        sb.append("bucket int NOT NULL, ");
        sb.append("replacement int NOT NULL ");
        sb.append(")");
        return sb.toString().intern();
    }

    private static String insertRouterNodesInBucketSQL(RouterBucket bucket) {
        if(bucket.getBucketSize() == 0) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO snapshot (node, address, port, latestResponse, failedResponses, bucket, replacement) VALUES ");
        Iterator<RouterNode> nodeIterator = bucket.getNodes().iterator();
        while(nodeIterator.hasNext()) {
            String delimiter = ", ";
            RouterNode node = nodeIterator.next();
            if(!nodeIterator.hasNext()) delimiter = "";

            sb.append("('"
                + node.getNode() + "', '"
                + node.getIPAddress().getHostAddress() + "', "
                + node.getPort() + ", "
                + node.getLatestResponse() + ", "
                + node.getFailedResponses() + ", "
                + bucket.getPrefixLength() + ", "
                + "0"
                + ")" + delimiter);
        }

        Iterator<RouterNode> replacementsIterator = bucket.getReplacementNodes().iterator();
        while(replacementsIterator.hasNext()) {
            RouterNode node = replacementsIterator.next();

            sb.append(", ('"
                + node.getNode() + "', '"
                + node.getIPAddress().getHostAddress() + "', "
                + node.getPort() + ", "
                + node.getLatestResponse() + ", "
                + node.getFailedResponses() + ", "
                + bucket.getPrefixLength() + ", "
                + "1"
                + ")");
        }

        return sb.toString();
    }

    private static String truncateOldSnapshotSQL() {
        return "DELETE FROM snapshot";
    }

    private static String getAllSnapshotNodes() {
        return "SELECT * FROM snapshot";
    }


}
