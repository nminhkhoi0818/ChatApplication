package ChatClient;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatClient {
    private final String serverName;
    private final int serverPort;
    private Socket socket;
    private OutputStream serverOut;
    private InputStream serverIn;
    private BufferedReader bufferedIn;

    private String login;

    public String getLogin() {
        return login;
    }

    public ArrayList<UserStatusListener> getUserStatusListeners() {
        return (ArrayList<UserStatusListener>) userStatusListeners;
    }

    private ArrayList<UserStatusListener> userStatusListeners = new ArrayList<>();
    private ArrayList<MessageListener> messageListeners = new ArrayList<>();

    private  ArrayList<ChatWindowListener> chatWindowListeners = new ArrayList<>();

    public ChatClient(String serverName, int serverPort) {
        this.serverName = serverName;
        this.serverPort = serverPort;
    }

    public void msg(String sendTo, String msgBody) throws IOException {
        String cmd = "msg " + sendTo + " " + msgBody + "\n";
        serverOut.write(cmd.getBytes());
    }

    public void msgGroup(String sendTo, String msgBody) throws IOException {
        String cmd = "msg-group " + sendTo + " " + msgBody + "\n";
        serverOut.write(cmd.getBytes());
    }

    public boolean login(String login, String password) throws IOException {
        String cmd = "login " + login + " " + password + "\n";
        serverOut.write(cmd.getBytes());;

        String response = bufferedIn.readLine();
        System.out.println("Response Line: " + response);

        if ("ok login".equalsIgnoreCase(response)) {
            this.login = login;
            return true;
        } else {
            return false;
        }
    }

    public boolean register(String login, String password) throws IOException {
        String cmd = "register " + login + " " + password + "\n";
        serverOut.write(cmd.getBytes());;

        String response = bufferedIn.readLine();

        return "ok register".equalsIgnoreCase(response);
    }

    public void getUsers(String login) throws IOException {
        String cmd = "users " + login + "\n";
        serverOut.write(cmd.getBytes());
    }

    public void getGroups(String login) throws IOException {
        String cmd = "groups " + login + "\n";
        serverOut.write(cmd.getBytes());
    }

    public void logoff() throws IOException {
        String cmd = "logoff\n";
        serverOut.write(cmd.getBytes());;
    }

    public void startMessageReader() {
        Thread t = new Thread() {
            @Override
            public void run() {
                readMessageLoop();
            }
        };
        t.start();
    }

    private void readMessageLoop() {
        try {
            String line;
            while ((line = bufferedIn.readLine()) != null) {
                String[] tokens = line.split(" ");
                if (tokens.length > 0) {
                    String cmd = tokens[0];
                    if ("online".equalsIgnoreCase(cmd)) {
                        handleOnline(tokens);
                    } else if ("offline".equalsIgnoreCase(cmd)) {
                        handleOffline(tokens);
                    } else if ("msg".equalsIgnoreCase(cmd)) {
                        String[] tokensMsg = line.split(" ", 3);
                        handleMessage(tokensMsg);
                    } else if("msg-group".equalsIgnoreCase(cmd)) {
                        String[] tokensMsg = line.split(" ", 4);
                        handleGroupMessage(tokensMsg);
                    } else if ("history".equalsIgnoreCase(cmd)) {
                        String[] tokensMsg = line.split(" ", 3);
                        handleHistoryMessage(tokensMsg);
                    } else if ("file".equalsIgnoreCase(cmd)) {
                        String[] tokensMsg = line.split(" ", 3);
                        handleFileMessage(tokensMsg);
                    } else if ("users".equalsIgnoreCase(cmd)) {
                        handleUserList(tokens);
                    } else if ("groups".equalsIgnoreCase(cmd)) {
                        handleGroupList(line);
                    } else if ("response-download".equalsIgnoreCase(cmd)) {
                        handleReposeDownload(tokens);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleReposeDownload(String[] tokens) throws IOException {
        long fileSize = Long.parseLong(tokens[2]);
        String filePath = tokens[3];

        FileOutputStream fos = new FileOutputStream(filePath);

        byte[] buffer = new byte[4096];
        int bytesRead;
        long totalBytesRead = 0;

        try {
            while ((bytesRead = serverIn.read(buffer)) != -1) {
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
    }

    private void handleGroupList(String line) {
        if (line != null) {
            String[] groupInfo = line.split(";");
            for (int i = 0; i < groupInfo.length; i++) {
                if (i != 0) {
                    String[] members = Arrays.copyOfRange(groupInfo[i].split(" "), 2, groupInfo[i].split(" ").length);
                    if (Arrays.asList(members).contains(login)) {
                        for (UserStatusListener listener : userStatusListeners) {
                            listener.addUser(groupInfo[i].split(" ")[1]);
                        }
                        for (ChatWindowListener listener : chatWindowListeners) {
                            listener.addUser(groupInfo[i].split(" ")[1], members);
                        }
                    }
                }
            }
        }
    }

    private void handleUserList(String[] tokens) {
        if (tokens.length > 1) {
            for (int i = 2; i < tokens.length; i++) {
                for (UserStatusListener listener : userStatusListeners) {
                    listener.addUser(tokens[i]);
                }
                for (ChatWindowListener listener : chatWindowListeners) {
                    listener.addUser(tokens[i], null);
                }
            }
        }
    }

    private void handleFileMessage(String[] tokens) throws IOException {
        String login = tokens[1];
        String msgBody = tokens[2];
        for (MessageListener listener : messageListeners) {
            listener.onMessage(login, msgBody, false, true, null);
        }
    }

    private void handleHistoryMessage(String[] tokensMsg) throws IOException {
        String login = tokensMsg[1];
        String msgBody = tokensMsg[2];
        for (MessageListener listener : messageListeners) {
            listener.onMessage(login, msgBody, true, false, null);
        }
    }

    private void handleMessage(String[] tokensMsg) throws IOException {
        String login = tokensMsg[1];
        String msgBody = tokensMsg[2];
        for (MessageListener listener : messageListeners) {
            listener.onMessage(login, msgBody, false, false, null);
        }
    }
    private void handleGroupMessage(String[] tokensMsg) throws IOException {
        String login = tokensMsg[1];
        String sender = tokensMsg[2];
        String msgBody = tokensMsg[3];
        for (MessageListener listener : messageListeners) {
            listener.onMessage(login, msgBody, false, false, sender);
        }
    }

    private void handleOffline(String[] tokens) {
        String login = tokens[1];
        for (UserStatusListener listener : userStatusListeners) {
            listener.offline(login);
        }
    }

    private void handleOnline(String[] tokens) {
        String login = tokens[1];
        for (UserStatusListener listener : userStatusListeners) {
            listener.online(login);
        }
    }

    public boolean connect() throws IOException {
        try {
            this.socket = new Socket(serverName, serverPort);
            System.out.println("Client port is " + socket.getLocalPort());
            this.serverOut = socket.getOutputStream();
            this.serverIn = socket.getInputStream();
            this.bufferedIn = new BufferedReader(new InputStreamReader(serverIn));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void addUserStatusListener(UserStatusListener listener) {
        userStatusListeners.add(listener);
    }

    public void removeUserStatusListener(UserStatusListener listener) {
        userStatusListeners.remove(listener);
    }

    public void addMessageListener(MessageListener listener) {
        messageListeners.add(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        messageListeners.remove(listener);
    }

    public void addChatWindowListener(ChatWindowListener listener) { chatWindowListeners.add(listener); }

    public void getMessageHistory(String sender, String receiver, boolean group) throws IOException {
        String cmd;
        if (group) {
            cmd = "history-group " + sender + " " +receiver + "\n";
        } else {
            cmd = "history " + sender + " " + receiver + "\n";
        }
        serverOut.write(cmd.getBytes());;
    }

    public void sendFile(String login, File file) throws IOException {
        if (file != null) {
            FileInputStream fis = new FileInputStream(file);

            long fileSize = file.length();

            serverOut.write(("file " + login + " " + file.getName() + " " + fileSize + "\n").getBytes());
            serverOut.flush();

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                serverOut.write(buffer, 0, bytesRead);
            }

            serverOut.flush();
            fis.close();
        }
    }

    public void createGroup(String groupName, List<String> users) throws IOException {
        String listUser = String.join(" ", users);
        serverOut.write(("create-group " + groupName + " " + listUser + "\n").getBytes());
    }

    public void requestDownloadFile(String login, String fileName, String filePath) throws IOException {
        serverOut.write(("request-download " + login + " " + fileName + " "  + filePath + "\n").getBytes());
    }
}
