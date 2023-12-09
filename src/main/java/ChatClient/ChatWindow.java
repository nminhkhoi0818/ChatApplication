package ChatClient;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.List;

public class ChatWindow extends JFrame {
    ChatClient client;
    String login;
    JPanel sideBar;
    JLabel userInfo;
    UserListPane userListPane;
    JButton logoffButton;
    JButton createGroupButton;
    // Sidebar
    JPanel mainContent;
    JLabel chattingWith;
    CardLayout cardLayout;
    JPanel cardPanel;

    ArrayList<MessagePane> messagePanes;

    ChatWindow(String login, ChatClient client) throws IOException {
        this.client = client;
        this.login = login;
        // Sidebar
        sideBar = new JPanel(new BorderLayout());
        userInfo = new JLabel("<html>Username: " + login + "<br>Port: " + client.getServerPort()+ "</html>");
        userListPane = new UserListPane(client, this);

        logoffButton = new JButton("Log Off");
        createGroupButton = new JButton("Create Group");
        JPanel buttonSideBarPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        buttonSideBarPanel.add(logoffButton);
        buttonSideBarPanel.add(createGroupButton);

        sideBar.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        sideBar.add(userInfo, BorderLayout.NORTH);
        sideBar.add(userListPane, BorderLayout.CENTER);
        sideBar.add(buttonSideBarPanel, BorderLayout.SOUTH);

        // Main content
        mainContent = new JPanel(new BorderLayout());
        chattingWith = new JLabel("");

        mainContent.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        mainContent.add(chattingWith, BorderLayout.NORTH);
        addAllMessagePanes();

        setLayout(new BorderLayout());
        add(sideBar, BorderLayout.WEST);
        add(mainContent, BorderLayout.CENTER);

        sideBar.setPreferredSize(new Dimension(150, getHeight()));
        mainContent.setPreferredSize(new Dimension(getWidth() - 150, getHeight()));

        this.setSize(600, 500);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    public void addAllMessagePanes() {
        messagePanes = new ArrayList<MessagePane>();
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        mainContent.add(cardPanel, BorderLayout.CENTER);

        try {
            List<String> users = client.getUsers(login);
            for (String user : users) {
                if (!Objects.equals(login, user)) {
                    MessagePane messagePane = new MessagePane(client, user);
                    messagePanes.add(messagePane);
                    cardPanel.add(messagePane, user);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showMessagePane(String login) {
        for (MessagePane messagePane : messagePanes) {
            if (login.equals(messagePane.getLogin())) {
                chattingWith.setText(login);
                cardLayout.show(cardPanel, login);
            }
        }
    }
}
