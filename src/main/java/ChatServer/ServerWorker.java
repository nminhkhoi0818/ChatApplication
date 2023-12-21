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

    InputStream inputStream;

    BufferedReader reader;
    DataInputStream fileIn;
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
        this.outputStream = clientSocket.getOutputStream();

        reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        fileIn = new DataInputStream(clientSocket.getInputStream());
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
                } else if ("msg-group".equalsIgnoreCase(cmd)) {
                    String[] tokenMsg = line.split(" ", 3);
                    handleGroupMessage(tokenMsg);
                } else if ("history".equalsIgnoreCase(cmd)) {
                    handleMessageHistory(tokens);
                } else if ("history-group".equalsIgnoreCase(cmd)) {
                    handleGroupMessageHistory(tokens);
                } else if ("users".equalsIgnoreCase(cmd)) {
                    handleGetAllUsers(tokens);
                } else if ("file".equalsIgnoreCase(cmd)) {
                    handleFileMessage(tokens);
                } else if ("create-group".equalsIgnoreCase(cmd)) {
                    String[] tokenMsg = line.split(" ", 3);
                    handleCreateGroup(tokenMsg);
                } else if ("groups".equalsIgnoreCase(cmd)) {
                    handleGetAllGroup(tokens);
                } else if ("request-download".equalsIgnoreCase(cmd)) {
                    String[] tokenDownload = line.split(" ", 4);
                    handleRequestDownload(tokenDownload);
                } else if ("delete-message".equalsIgnoreCase(cmd)) {
                    handleDeleteMessage(tokens);
                } else if ("delete-group-message".equalsIgnoreCase(cmd)) {
                    handleDeleteGroupMessage(tokens);
                }
                else {
                    String msg = "unknown " + cmd + "\n";
                    outputStream.write(msg.getBytes());
                }
            }
        }
        clientSocket.close();
    }

    private void handleDeleteGroupMessage(String[] tokens) {
        server.getDatabaseHelper().deleteGroupMessage(Integer.parseInt(tokens[1]));
    }

    private void handleDeleteMessage(String[] tokens) {
        server.getDatabaseHelper().deleteMessage(Integer.parseInt(tokens[1]));
    }

    private void handleRequestDownload(String[] tokens) throws IOException, InterruptedException {
        String sendTo = tokens[1];
        String fileName = tokens[2];
        String filePath = tokens[3];

        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);

        List<ServerWorker> workerList = server.getWorkerList();
        for (ServerWorker worker : workerList) {
            if (sendTo.equalsIgnoreCase(worker.getLogin())) {
                String outMsg = "response-download " + login + " " + file.length() + " " + filePath + "\n";
                worker.send(outMsg);
                worker.outputStream.flush();

                Thread.sleep(1000);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    worker.outputStream.write(buffer, 0, bytesRead);
                }

                worker.outputStream.flush();
                fis.close();
            }
        }
        fis.close();
    }

    private void handleGroupMessage(String[] tokenMsg) throws IOException {
        server.getDatabaseHelper().sendGroupMessage(login, tokenMsg[1], tokenMsg[2]);
        List<String> members = server.getDatabaseHelper().getMembersByGroupName(tokenMsg[1]);
        List<ServerWorker> workerList = server.getWorkerList();
        for (ServerWorker worker : workerList) {
            if (members.contains(worker.getLogin())) {
                String outMsg = "msg-group " + tokenMsg[1] + " " + login + " " + tokenMsg[2] + "\n";
                worker.send(outMsg);
            }
        }
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

    public void handleFileMessage(String[] tokens) throws IOException, InterruptedException {
        String sendTo = tokens[1];
        String fileName = tokens[2];

        long fileSize = fileIn.readLong();

        File file = new File(fileName);
        file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);
        byte[] buffer = new byte[4096];

        int bytesRead = 0;
        long totalBytesRead = 0;

        try {
            while ((bytesRead = fileIn.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
                System.out.println(bytesRead);
                totalBytesRead += bytesRead;
                fos.write(buffer, 0, bytesRead);
                if (totalBytesRead >= fileSize) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        fos.close();

        List<ServerWorker> workerList = server.getWorkerList();
        for (ServerWorker worker : workerList) {
            if (sendTo.equalsIgnoreCase(worker.getLogin())) {
                String outMsg = "file " + login + " " + fileName + "\n";
                worker.send(outMsg);
            }
        }
    }

    private void handleMessage(String[] tokens) throws IOException {
        String sendTo = tokens[1];
        String body = tokens[2];
        List<ServerWorker> workerList = server.getWorkerList();
        server.getDatabaseHelper().insertMessage(login, sendTo, body);
        for (ServerWorker worker : workerList) {
            if (sendTo.equalsIgnoreCase(worker.getLogin())) {
                String outMsg = "msg " + login + " " +  body + "\n";
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

                List<ServerWorker> workerList = server.getWorkerList();

                // send current user all other online logins
                for (ServerWorker worker : workerList) {
                    if (worker.getLogin() != null) {
                        if (!login.equals(worker.getLogin())) {
                            String msg2 = "online " + worker.getLogin() + "\n";
                            send(msg2);
                        }
                    }
                }

                // send other online users current user's status
                String onlineMsg = "online " + login + "\n";
                for (ServerWorker worker : workerList) {
                    if (!login.equals(worker.getLogin())) {
                        worker.send(onlineMsg);
                    }
                }
            } else {
                String msg = "error login\n";
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
                    worker.send(outMsg);
                }
            }
        }
    }

    private void handleGroupMessageHistory(String[] tokens) throws IOException {
        String sender = tokens[1];
        String groupName = tokens[2];
        List<String> chatHistory = server.getDatabaseHelper().getGroupMessages(groupName);

        List<ServerWorker> workerList = server.getWorkerList();
        for (ServerWorker worker : workerList) {
            if (sender.equalsIgnoreCase(worker.getLogin())) {
                for (String msg : chatHistory) {
                    String outMsg = "history " + groupName + " " + msg + "\n";
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
