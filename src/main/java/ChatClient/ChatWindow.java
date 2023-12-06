package ChatClient;

import javax.swing.*;
import java.awt.*;

public class ChatWindow extends JFrame {
    JPanel sideBar;
    JLabel userInfo;
    DefaultListModel onlineUserModel;
    JList<String> onlineList;
    JButton logoffButton;
    JButton createGroupButton;
    // Sidebar
    JPanel mainContent;
    JLabel chattingWith;
    TextField messageArea;
    TextField messageSend;

    JButton sendButton;
    JButton fileButton;
    ChatWindow() {
        // Sidebar
        sideBar = new JPanel(new BorderLayout());
        userInfo = new JLabel("<html>Username: Minh Khoi<br>Port: 8818</html>");
        onlineUserModel = new DefaultListModel();
        onlineList = new JList<>(onlineUserModel);
        logoffButton = new JButton("Logoff");
        createGroupButton = new JButton("Create Group");
        JPanel buttonSideBarPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        buttonSideBarPanel.add(logoffButton);
        buttonSideBarPanel.add(createGroupButton);

        sideBar.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        sideBar.add(userInfo, BorderLayout.NORTH);
        sideBar.add(new JScrollPane(onlineList), BorderLayout.CENTER);
        sideBar.add(buttonSideBarPanel, BorderLayout.SOUTH);

        // Main content
        mainContent = new JPanel(new BorderLayout());
        chattingWith = new JLabel("Guest");
        messageArea = new TextField();
        messageArea.setEnabled(false);
        messageSend = new TextField();
        sendButton = new JButton("Send");
        fileButton = new JButton("File");
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(messageSend, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        buttonPanel.add(sendButton);
        buttonPanel.add(fileButton);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        mainContent.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        mainContent.add(chattingWith, BorderLayout.NORTH);
        mainContent.add(new JScrollPane(messageArea), BorderLayout.CENTER);
        mainContent.add(inputPanel, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(sideBar, BorderLayout.WEST);
        add(mainContent, BorderLayout.CENTER);

        sideBar.setPreferredSize(new Dimension(150, getHeight()));
        mainContent.setPreferredSize(new Dimension(getWidth() - 150, getHeight()));

        this.setSize(600, 500);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    public static void main(String[] args) {
        new ChatWindow();
    }
}
