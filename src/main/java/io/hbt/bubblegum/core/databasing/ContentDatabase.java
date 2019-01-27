package io.hbt.bubblegum.core.databasing;

import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class ContentDatabase {
    private static ContentDatabase instance;
    private static Connection connection;

    private ContentDatabase() {

    }

    protected synchronized static ContentDatabase getInstance() {
        if(ContentDatabase.instance == null) ContentDatabase.instance = new ContentDatabase();
        return ContentDatabase.instance;
    }

    private void openDatabaseConnection() {

        if(ContentDatabase.connection != null) {
            try {
                ContentDatabase.connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        try {
            // create a databasing connection
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + Configuration.DB_FOLDER_PATH + Configuration.CDB_FILE_NAME);

            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.
            statement.executeUpdate(this.initContentDatabaseSQL());
            statement = null;

            ContentDatabase.connection = connection;

        } catch (SQLException e) {
            // if the error message is "out of memory",
            // it probably means no databasing file is found
            System.err.println(e.getMessage());
            ContentDatabase.connection = null;
        }
    }

    private Post saveEntity(String id, BubblegumNode node, String content) {
        return this.saveEntity(id, node, content, "");
    }

    private Post saveEntity(String id, BubblegumNode node, String content, String response) {
        if(content == null || node == null) return null;

        if(ContentDatabase.connection == null) this.openDatabaseConnection();
        if(ContentDatabase.connection == null) return null; // Database connection failed, don't try to save anything

        String network = node.getNetworkIdentifier();
        String owner = node.getNodeIdentifier().toString();

        PreparedStatement ps = null;
        try {
            ps = ContentDatabase.connection.prepareStatement(this.createPostSQL());
            ps.setString(1, id);
            ps.setString(2, owner);
            ps.setString(3, network);
            ps.setString(4, content);
            ps.setString(5, response);
            ps.setLong(6, System.currentTimeMillis());
            int numInserted = ps.executeUpdate();

            if(numInserted == 1) {
                return new Post(id, owner, network, content, response, System.currentTimeMillis());
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Post savePost(BubblegumNode node, String content, String inResponseTo) {
        return this.saveEntity("post-"+UUID.randomUUID().toString(), node, content, inResponseTo);
    }

    public Post saveMeta(String key, BubblegumNode node, String content) {
        return this.saveEntity("_"+ key + "_" + node.getNodeIdentifier().toString(), node, content);
    }

    public Post getPost(BubblegumNode node, String id) {
        if(id == null || node == null) return null;

        if(ContentDatabase.connection == null) this.openDatabaseConnection();
        if(ContentDatabase.connection == null) return null; // Database connection failed, don't try to save anything

        PreparedStatement ps;
        try {
            ps = ContentDatabase.connection.prepareStatement(this.getPostSQL());
            ps.setString(1, id);
            ps.setString(2, node.getIdentifier());
            ps.setString(3, node.getNetworkIdentifier());
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                String content = rs.getString("content");
                String response = rs.getString("response");
                long timeCreated = rs.getLong("time_created");
                return new Post(id, node.getNodeIdentifier().toString(), node.getNetworkIdentifier(), content, response, timeCreated);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<Post> getPosts(BubblegumNode node) {
        if(node == null) return new ArrayList<>();

        if(ContentDatabase.connection == null) this.openDatabaseConnection();
        if(ContentDatabase.connection == null) return new ArrayList<>(); // Database connection failed, don't try to save anything

        PreparedStatement ps;
        try {
            ps = ContentDatabase.connection.prepareStatement(this.getAllPostsSQL());
            ps.setString(1, node.getIdentifier());
            ps.setString(2, node.getNetworkIdentifier());
            ResultSet rs = ps.executeQuery();

            ArrayList<Post> posts = new ArrayList<>();
            while(rs.next()) {
                String id = rs.getString("id");
                String content = rs.getString("content");
                String response = rs.getString("response");
                long timeCreated = rs.getLong("time_created");
                posts.add(new Post(id, node.getNodeIdentifier().toString(), node.getNetworkIdentifier(), content, response, timeCreated));
            }

            return posts;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    public List<Post> queryPosts(BubblegumNode node, long start, long end, List<String> ids) {
        if(ids != null && ids.size() > 0) return queryPostsByIDs(node, ids);
        return queryPostsByTime(node, start, end);
    }

    public List<Post> queryPostsByTime(BubblegumNode node, long start, long end) {
        if(start < 0 || end < 0 || end < start) return new ArrayList<>();

        if(ContentDatabase.connection == null) this.openDatabaseConnection();
        if(ContentDatabase.connection == null) return new ArrayList<>(); // Database connection failed, don't try to retrieve anything

        PreparedStatement ps;
        try {
            ps = ContentDatabase.connection.prepareStatement(this.queryPostsSQLByTime());
            ps.setLong(1, start);
            ps.setLong(2, end);
            ResultSet rs = ps.executeQuery();

            ArrayList<Post> posts = new ArrayList<>();
            while(rs.next()) {
                String content = rs.getString("content");
                String response = rs.getString("response");
                long timeCreated = rs.getLong("time_created");
                String postID = rs.getString("id");
                posts.add(new Post(postID, node.getNodeIdentifier().toString(), node.getNetworkIdentifier(), content, response, timeCreated));
            }

            return posts;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    public List<Post> queryPostsByIDs(BubblegumNode node, List<String> ids) {
        if(ids == null || ids.size() == 0) return new ArrayList<>();

        if(ContentDatabase.connection == null) this.openDatabaseConnection();
        if(ContentDatabase.connection == null) return new ArrayList<>(); // Database connection failed, don't try to retrieve anything

        PreparedStatement ps;
        try {
            ps = ContentDatabase.connection.prepareStatement(this.queryPostsSQLByIDs(ids.size()));
            for(int i = 1; i <= ids.size(); i++) ps.setString(i, ids.get(i-1));
            ResultSet rs = ps.executeQuery();

            ArrayList<Post> posts = new ArrayList<>();
            while(rs.next()) {
                String content = rs.getString("content");
                String response = rs.getString("response");
                long timeCreated = rs.getLong("time_created");
                String postID = rs.getString("id");
                posts.add(new Post(postID, node.getNodeIdentifier().toString(), node.getNetworkIdentifier(), content, response, timeCreated));
            }

            return posts;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    /** SQL **/

    private String initContentDatabaseSQL() {
        /* create table if not exists networks( id varchar(255) NOT NULL, hash varchar(255) NOT NULL, port int NOT NULL, CONSTRAINT key PRIMARY KEY (id,hash) ) */
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS _posts( ");
        sb.append("id VARCHAR(255) NOT NULL, ");
        sb.append("owner VARCHAR(255) NOT NULL, ");
        sb.append("network VARCHAR(255) NOT NULL, ");
        sb.append("content TEXT NOT NULL, ");
        sb.append("response TEXT NOT NULL, ");
        sb.append("time_created INTEGER NOT NULL, ");
        sb.append("PRIMARY KEY (id) ");
        sb.append(")");
        return sb.toString();
    }

    private String createPostSQL() {
        StringBuilder sb = new StringBuilder();
        sb.append("REPLACE INTO _posts ");
        sb.append("(id, owner, network, content, response, time_created) ");
        sb.append("VALUES (?, ?, ?, ?, ?, ?)");
        return sb.toString();
    }

    private String postExistencecheckSQL() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT EXISTS( ");
        sb.append("SELECT 1 FROM _posts WHERE id=? LIMIT 1 ");
        sb.append(")");
        return sb.toString();
    }

    private String updateNetworkDetailsSQL() {
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE networks ");
        sb.append("SET content=?");
        sb.append("WHERE id=? AND owner=? AND network=? ");
        return sb.toString();
    }

    private String getPostSQL() {
        return "SELECT content, response, time_created FROM _posts WHERE id=? AND owner=? AND network=? LIMIT 1";
    }

    private String getAllPostsSQL() {
        return "SELECT id, content, response, time_created FROM _posts WHERE owner=? AND network=?";
    }

    private String queryPostsSQLByTime() {
        return "SELECT id, content, response, time_created FROM _posts WHERE time_created >= ? AND time_created <= ?";
    }

    private String queryPostsSQLByIDs(int params) {
        String[] paramString = new String[params];
        for(int i = 0; i < params; i++) paramString[i] = "?";
        return "SELECT id, content, response, time_created FROM _posts WHERE id IN (" + String.join(",", paramString) + ")";
    }
}
