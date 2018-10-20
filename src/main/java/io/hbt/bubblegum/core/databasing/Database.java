package io.hbt.bubblegum.core.databasing;

import io.hbt.bubblegum.core.kademlia.BubblegumNode;

import java.io.File;
import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;

public class Database {

    private HashMap<String, byte[]> db = new HashMap<>();
    private BubblegumNode localNode;

    public Database(BubblegumNode localNode) {
        this.localNode = localNode;

        Connection connection = null;
        try
        {
            this.checkDatabasesDirectory();

            // create a databasing connection
            connection = DriverManager.getConnection("jdbc:sqlite:databases/" + this.localNode.getNodeIdentifier().toString() + ".db");
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.

//            statement.executeUpdate("drop table if exists person");
//            statement.executeUpdate("create table person (id integer, name string)");
//            statement.executeUpdate("insert into person values(1, 'leo')");
//            statement.executeUpdate("insert into person values(2, 'yui')");
//            ResultSet rs = statement.executeQuery("select * from person");
//            while(rs.next())
//            {
//                // read the result set
//                System.out.println("name = " + rs.getString("name"));
//                System.out.println("id = " + rs.getInt("id"));
//            }
        }
        catch(SQLException e)
        {
            // if the error message is "out of memory",
            // it probably means no databasing file is found
            System.err.println(e.getMessage());
        }
        finally
        {
            try
            {
                if(connection != null)
                    connection.close();
            }
            catch(SQLException e)
            {
                // connection close failed.
                System.err.println(e);
            }
        }

    }

    public boolean hasKey(String key) {
        return this.db.containsKey(key);
    }

    public byte[] valueForKey(String key) {
        return this.db.get(key);
    }

    public boolean add(String key, byte[] value) {
        this.db.put(key, value);
        this.print("[Database] Saved " + key + " -> " + Arrays.toString(value));
        return true;
    }

    private void checkDatabasesDirectory() {
        File directory = new File("databases");
        if (! directory.exists()) directory.mkdir();
    }

    private void print(String msg) {
        this.localNode.log(msg);
    }
}
