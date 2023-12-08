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

    public void insertMessage(String sender, String receiver, String message) {
        String sql = "INSERT INTO messages (sender, receiver, message) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, sender);
            pstmt.setString(2, receiver);
            pstmt.setString(3, message);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
                    String timestamp = resultSet.getString("timestamp");
                    String messageSender = resultSet.getString("sender");
                    String messageReceiver = resultSet.getString("receiver");
                    String messageBody = resultSet.getString("message");

                    String formattedMessage = String.format("%s %s %s", messageSender, messageReceiver, messageBody);
                    messages.add(formattedMessage);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return messages;
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

    public static void main(String args[]) {
        new DatabaseHelper();
    }
}