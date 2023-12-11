package ChatServer;

import javax.swing.plaf.synth.SynthDesktopIconUI;
import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class ServerWorker extends Thread {
    private final Socket clientSocket;
    private final Server server;
    private String login = null;
    private OutputStream outputStream;
    private HashSet<String> topicSet = new HashSet<>();
    private List<String> fileNameList = new ArrayList<>();
    public ServerWorker(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            handleClientSocket(clientSocket);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleClientSocket(Socket clientSocket) throws IOException, InterruptedException, SQLException {
        InputStream inputStream = clientSocket.getInputStream();
        this.outputStream = clientSocket.getOutputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] tokens = line.split(" ");
            if (tokens.length > 0) {
                String cmd = tokens[0];
                if ("logoff".equalsIgnoreCase(cmd) || "quit".equalsIgnoreCase(cmd)) {
                    handleLogoff();
                    break;
                } else if ("login".equalsIgnoreCase(cmd)) {
                    handleLogin(outputStream, tokens);
                } else if ("register".equalsIgnoreCase(cmd)) {
                    handleRegister(outputStream, tokens);
                }
                else if ("msg".equalsIgnoreCase(cmd)) {
                    String[] tokenMsg = line.split(" ", 3);
                    handleMessage(tokenMsg);
                } else if ("join".equalsIgnoreCase(cmd)) {
                    handleJoin(tokens);
                } else if ("leave".equalsIgnoreCase(cmd)) {
                    handleLeave(tokens);
                } else if ("history".equalsIgnoreCase(cmd)) {
                    handleMessageHistory(tokens);
                } else if ("users".equalsIgnoreCase(cmd)) {
                    handleGetAllUsers(tokens);
                } else if ("file".equalsIgnoreCase(cmd)) {
                    handleFileMessage(tokens);
                } else if ("check".equalsIgnoreCase(cmd)) {
                    checkExistFileName(tokens);
                } else if ("create-group".equalsIgnoreCase(cmd)) {
                    String[] tokenMsg = line.split(" ", 3);
                    handleCreateGroup(tokenMsg);
                } else if ("groups".equalsIgnoreCase(cmd)) {
                    handleGetAllGroup(tokens);
                }
                else {
                    String msg = "unknown " + cmd + "\n";
                    outputStream.write(msg.getBytes());
                }
            }
        }
        clientSocket.close();
    }

    private void handleGetAllGroup(String[] tokens) throws IOException {
        String allGroups = String.valueOf(server.getDatabaseHelper().getAllGroupsInfo());
        if (allGroups != null) {
            String groupListMsg = "groups " + tokens[1] + ";" + allGroups + "\n";
            outputStream.write(groupListMsg.getBytes());
        }
    }

    private void handleCreateGroup(String[] tokens) {
        String groupName = tokens[1];
        List<String> groupUsers = Arrays.asList(tokens[2].split(" "));
        server.getDatabaseHelper().createGroup(groupName, groupUsers);
    }

    public void handleFileMessage(String[] tokens) throws IOException {
        InputStream inputStream = clientSocket.getInputStream();

        String sendTo = tokens[1];
        String fileName = tokens[2];
        long fileSize = Long.parseLong(tokens[3]);

        FileOutputStream fos = new FileOutputStream("files/" + fileName);
        BufferedOutputStream bos = new BufferedOutputStream(fos);

        byte[] buffer = new byte[4096];
        int bytesRead;
        long totalBytesRead = 0;

        try {
            while ((totalBytesRead < fileSize) && (bytesRead = inputStream.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                bos.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        bos.close();
        fos.close();
    }

    private void handleLeave(String[] tokens) {
        if (tokens.length > 1) {
            String topic = tokens[1];
            topicSet.remove(topic);
        }
    }

    public boolean isMemberOfTopic(String topic) {
        return topicSet.contains(topic);
    }

    private void handleJoin(String[] tokens) {
        if (tokens.length > 1) {
            String topic = tokens[1];
            topicSet.add(topic);
        }
    }

    private void handleMessage(String[] tokens) throws IOException {
        String sendTo = tokens[1];
        String body = tokens[2];

        List<ServerWorker> workerList = server.getWorkerList();
        server.getDatabaseHelper().insertMessage(login, sendTo, body);
        for (ServerWorker worker : workerList) {
            if (sendTo.equalsIgnoreCase(worker.getLogin())) {
                String outMsg = "msg " + login + " " + body + "\n";
                worker.send(outMsg);
            }
        }
    }

    private void handleLogoff() throws IOException {
        server.removeWorker(this);
        List<ServerWorker> workerList = server.getWorkerList();

        // send other online users current user's status
        String onlineMsg = "offline " + login + "\n";
        for (ServerWorker worker : workerList) {
            if (!login.equals(worker.getLogin())) {
                worker.send(onlineMsg);
            }
        }
        clientSocket.close();
    }

    public void checkExistFileName(String[] tokens) throws IOException {
        String fileName = tokens[2];
        String login = tokens[1];
        List<ServerWorker> workerList = server.getWorkerList();
        for (String fName : fileNameList) {
            if (fileName.equalsIgnoreCase(fName)) {
                for (ServerWorker worker : workerList) {
                    if (login.equalsIgnoreCase(worker.getLogin())) {
                       worker.send("ok exist");
                    }
                }
                return;
            }
        }
    }

    public String getLogin() {
        return login;
    }

    private void handleLogin(OutputStream outputStream, String[] tokens) throws IOException {
        if (tokens.length == 3) {
            String login = tokens[1];
            String password = tokens[2];
            if (server.getDatabaseHelper().authenticateUser(login, password)) {
                String msg = "ok login\n";
                outputStream.write(msg.getBytes());
                this.login = login;
                System.out.println("User logged in successfully " + login);

//                List<ServerWorker> workerList = server.getWorkerList();
//
//                // send current user all other online logins
//                for (ServerWorker worker : workerList) {
//                    if (worker.getLogin() != null) {
//                        if (!login.equals(worker.getLogin())) {
//                            String msg2 = "online " + worker.getLogin() + "\n";
//                            send(msg2);
//                        }
//                    }
//                }
//
//                // send other online users current user's status
//                String onlineMsg = "online " + login + "\n";
//                for (ServerWorker worker : workerList) {
//                    if (!login.equals(worker.getLogin())) {
//                        worker.send(onlineMsg);
//                    }
//                }
            } else {
                String msg = "error login";
                outputStream.write(msg.getBytes());
                System.err.println("Login failed for " + login);
            }
        }
    }

    private void handleRegister(OutputStream outputStream, String[] tokens) throws IOException {
        if (tokens.length == 3) {
            String username = tokens[1];
            String password = tokens[2];

            DatabaseHelper databaseHelper = new DatabaseHelper();

            // Check if the user already exists
            if (databaseHelper.authenticateUser(username, password)) {
                String msg = "error register\n";
                outputStream.write(msg.getBytes());
                System.err.println("Registration failed for " + username + ". User already exists.");
            } else {
                databaseHelper.registerUser(username, password);
                String msg = "ok register\n";
                outputStream.write(msg.getBytes());
                System.out.println("User registered successfully: " + username);
            }
            databaseHelper.close();
        }
    }

    private void handleGetAllUsers(String[] tokens) throws IOException {
        List<String> allUsers = server.getDatabaseHelper().getAllUsers();
        if (allUsers != null) {
            String userListMsg = "users " + tokens[1] + " " + String.join(" ", allUsers) + "\n";
            outputStream.write(userListMsg.getBytes());
        }
    }

    private void handleMessageHistory(String[] tokens) throws SQLException, IOException {
        String sender = tokens[1];
        String receiver = tokens[2];

        List<String> chatHistory = server.getDatabaseHelper().getChatHistory(sender, receiver);
        List<ServerWorker> workerList = server.getWorkerList();
        for (ServerWorker worker : workerList) {
            if (sender.equalsIgnoreCase(worker.getLogin())) {
                for (String msg : chatHistory) {
                    String outMsg = "history " + receiver + " " + msg + "\n";
                    System.out.println(outMsg);
                    worker.send(outMsg);
                }
            }
        }
    }

    private void send(String msg) throws IOException {
        if (login != null) {
            outputStream.write(msg.getBytes());
        }
    }
}
