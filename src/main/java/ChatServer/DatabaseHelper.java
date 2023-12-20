package ChatServer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {
    private Connection connection;

    public DatabaseHelper() {
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection("jdbc:postgresql://dpg-clfb5hdadtrs73eajc20-a.oregon-postgres.render.com/week8db?user=nmkhoi&password=FZZq1J7lSVc8xatyiMJohFh4mOi6NeLV");
            createUsersTable();
            createTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Opened database successfully");
    }

    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS messages (" +
                "id SERIAL PRIMARY KEY," +
                "sender VARCHAR(255) NOT NULL," +
                "receiver VARCHAR(255) NOT NULL," +
                "message TEXT NOT NULL," +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        Statement stmt = connection.createStatement();
        stmt.execute(sql);
    }

    private void createUsersTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "id SERIAL PRIMARY KEY," +
                "username VARCHAR(255) UNIQUE NOT NULL," +
                "password VARCHAR(255) NOT NULL" +
                ")";
        Statement stmt = connection.createStatement();
        stmt.execute(sql);
    }

    public void registerUser(String username, String password) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean authenticateUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            try (ResultSet resultSet = pstmt.executeQuery()) {
                return resultSet.next(); // True if user is authenticated, false otherwise
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Integer insertMessage(String sender, String receiver, String message) {
        String sql = "INSERT INTO messages (sender, receiver, message) VALUES (?, ?, ?)";
        int messageId = -1;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, sender);
            pstmt.setString(2, receiver);
            pstmt.setString(3, message);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return messageId;
    }

    public List<String> getChatHistory(String sender, String receiver) {
        List<String> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) ORDER BY timestamp";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, sender);
            pstmt.setString(2, receiver);
            pstmt.setString(3, receiver);
            pstmt.setString(4, sender);
            try (ResultSet resultSet = pstmt.executeQuery()) {
                while (resultSet.next()) {
                    int messageId = resultSet.getInt("id");
                    String timestamp = resultSet.getString("timestamp");
                    String messageSender = resultSet.getString("sender");
                    String messageReceiver = resultSet.getString("receiver");
                    String messageBody = resultSet.getString("message");

                    String formattedMessage = String.format("%d %s %s", messageId, messageSender, messageBody);
                    messages.add(formattedMessage);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return messages;
    }

    public List<String> getAllUsers() {
        List<String> users = new ArrayList<>();
        String sql = "SELECT username FROM users";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet resultSet = pstmt.executeQuery()) {
            while (resultSet.next()) {
                String username = resultSet.getString("username");
                users.add(username);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return users;
    }

    public int getUserIdByUsername(String username) {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            } else {
                return -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private int getGroupIdByName(String groupName) {
        String sql = "SELECT id FROM groups WHERE group_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, groupName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            } else {
                return -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void createGroup(String groupName, List<String> members) {
        String insertGroup = "INSERT INTO groups (group_name) VALUES (?) RETURNING id";
        String insertMembers = "INSERT INTO group_members (group_id, user_id) VALUES (?, ?)";

        try (PreparedStatement pstmtGroup = connection.prepareStatement(insertGroup, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pstmtMember = connection.prepareStatement(insertMembers)) {

            pstmtGroup.setString(1, groupName);
            pstmtGroup.executeUpdate();
            ResultSet rsGroup = pstmtGroup.getGeneratedKeys();
            if (rsGroup.next()) {
                int groupId = rsGroup.getInt(1);

                for (String member : members) {
                    int userId = getUserIdByUsername(member);
                    pstmtMember.setInt(1, groupId);
                    pstmtMember.setInt(2, userId);
                    pstmtMember.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public StringBuilder getAllGroupsInfo() {
        StringBuilder allGroupsInfo = new StringBuilder();

        String getAllGroupsQuery = "SELECT * FROM groups";
        try (Statement stmt = connection.createStatement();
             ResultSet rsGroups = stmt.executeQuery(getAllGroupsQuery)) {
            while (rsGroups.next()) {
                int groupId = rsGroups.getInt("id");
                String groupName = rsGroups.getString("group_name");

                allGroupsInfo.append(groupId).append(" ").append(groupName).append(" ");

                String getMembers = "SELECT username FROM users INNER JOIN group_members ON users.id = group_members.user_id WHERE group_members.group_id = ?";
                try (PreparedStatement pstmtMembers = connection.prepareStatement(getMembers)) {
                    pstmtMembers.setInt(1, groupId);
                    ResultSet rsMembers = pstmtMembers.executeQuery();
                    boolean isFirst = true;
                    while (rsMembers.next()) {
                        if (isFirst) {
                            allGroupsInfo.append(rsMembers.getString("username"));
                            isFirst = false;
                        } else {
                            allGroupsInfo.append(" ").append(rsMembers.getString("username"));
                        }
                    }
                }
                allGroupsInfo.append(";");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return allGroupsInfo;
    }

    public List<String> getMembersByGroupName(String groupName) {
        List<String> members = new ArrayList<>();
        String getGroupId = "SELECT id FROM groups WHERE group_name = ?";
        String getMembers = "SELECT username FROM users INNER JOIN group_members ON users.id = group_members.user_id WHERE group_members.group_id = ?";

        try (PreparedStatement pstmtGroup = connection.prepareStatement(getGroupId)) {
            pstmtGroup.setString(1, groupName);
            ResultSet rsGroup = pstmtGroup.executeQuery();
            if (rsGroup.next()) {
                int groupId = rsGroup.getInt("id");

                try (PreparedStatement pstmtMembers = connection.prepareStatement(getMembers)) {
                    pstmtMembers.setInt(1, groupId);
                    ResultSet rsMembers = pstmtMembers.executeQuery();
                    while (rsMembers.next()) {
                        members.add(rsMembers.getString("username"));
                    }
                }
            } else {
                System.out.println("Group not found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }

    public void sendGroupMessage(String senderName, String groupName, String message) {
        int groupId = getGroupIdByName(groupName);
        if (groupId == -1) {
            System.out.println("Group not found.");
            return;
        }

        int senderId = getUserIdByUsername(senderName);
        if (senderId == -1) {
            System.out.println("User not found.");
            return;
        }
        String sql = "INSERT INTO group_messages (group_id, sender_id, message) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            pstmt.setInt(2, senderId);
            pstmt.setString(3, message);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getGroupMessages(String groupName) {
        List<String> messages = new ArrayList<>();
        int groupId = getGroupIdByName(groupName);
        if (groupId == -1) {
            System.out.println("Group not found.");
            return messages;
        }
        String sql = "SELECT users.username, group_messages.message, group_messages.id FROM group_messages " +
                "INNER JOIN users ON group_messages.sender_id = users.id " +
                "WHERE group_messages.group_id = ? ORDER BY group_messages.timestamp ASC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int messageId = rs.getInt("id");
                String username = rs.getString("username");
                String message = rs.getString("message");
                messages.add(messageId + " " + username + " " + message);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }
    public void deleteGroupMessage(int messageId) {
        String sql = "DELETE FROM group_messages WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, messageId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void createGroupChatTables() {
        String createGroupsTable = "CREATE TABLE IF NOT EXISTS groups (" +
                "id SERIAL PRIMARY KEY," +
                "group_name VARCHAR(255) UNIQUE NOT NULL" +
                ")";

        String createGroupMembersTable = "CREATE TABLE IF NOT EXISTS group_members (" +
                "id SERIAL PRIMARY KEY," +
                "group_id INT NOT NULL REFERENCES groups(id)," +
                "user_id INT NOT NULL REFERENCES users(id)," +
                "UNIQUE(group_id, user_id)" +
                ")";

        String createGroupMessagesTable = "CREATE TABLE IF NOT EXISTS group_messages (" +
                "id SERIAL PRIMARY KEY," +
                "group_id INT NOT NULL REFERENCES groups(id)," +
                "sender_id INT NOT NULL REFERENCES users(id)," +
                "message TEXT NOT NULL," +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createGroupsTable);
            stmt.execute(createGroupMembersTable);
            stmt.execute(createGroupMessagesTable);
            System.out.println("Group chat tables created successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteMessage(int messageId) {
        String sql = "DELETE FROM messages WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, messageId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void clearTableData(String tableName) {
        String sql = "DELETE FROM " + tableName;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("All data from " + tableName + " has been cleared.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        DatabaseHelper databaseHelper = new DatabaseHelper();
        databaseHelper.clearTableData("messages");
        databaseHelper.clearTableData("groups");
        databaseHelper.createGroupChatTables();
    }
}