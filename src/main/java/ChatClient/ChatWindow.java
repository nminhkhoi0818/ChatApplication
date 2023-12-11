package ChatClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
        buttonSideBarPanel.add(createGroupButton);
        buttonSideBarPanel.add(logoffButton);
        List<String> users = client.getUsers(login);
        createGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog chooseUserDialog = new JDialog();
                JPanel chooseUserContent = new JPanel();
                chooseUserContent.setLayout(new BorderLayout());

                JPanel userListPanel = new JPanel();
                userListPanel.setLayout(new BorderLayout());
                DefaultListModel<String> listModel = new DefaultListModel<>();
                for (String user : users) {
                    listModel.addElement(user);
                }
                JList<String> userJList = new JList<>(listModel);
                JScrollPane userScrollPanel = new JScrollPane(userJList);
                userListPanel.add(userScrollPanel, BorderLayout.CENTER);
                chooseUserContent.add(userListPanel, BorderLayout.NORTH);

                JPanel centerPanel = new JPanel();
                centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

                JLabel groupNameLabel = new JLabel("Group name: ");
                JTextField groupNameField = new JTextField();
                centerPanel.add(groupNameLabel);
                centerPanel.add(groupNameField);


                centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                chooseUserContent.add(centerPanel, BorderLayout.CENTER);

                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                JButton createButton = new JButton("Create group");
                createButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String groupName = groupNameField.getText();
                        if (groupName.isEmpty()) {
                            JOptionPane.showMessageDialog(chooseUserDialog, "Group name must not empty", "Warning",JOptionPane.WARNING_MESSAGE);
                        }
                        List<String> chosenUsers = userJList.getSelectedValuesList();
                        if (chosenUsers.size() < 2) {
                            JOptionPane.showMessageDialog(chooseUserDialog, "At least 3 members to create group", "Warning", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                        try {
                            client.createGroup(groupName, chosenUsers);
                            userListPane.addGroup(groupName);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        chooseUserDialog.setVisible(false);
                        chooseUserDialog.dispose();
                    }
                });
                buttonPanel.add(createButton);
                chooseUserContent.add(buttonPanel, BorderLayout.SOUTH);

                chooseUserDialog.setMinimumSize(new Dimension(300, 150));
                chooseUserDialog.setContentPane(chooseUserContent);
                chooseUserDialog.pack();
                chooseUserDialog.setVisible(true);
            }
        });

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
        boolean firstUser = false;
        try {
            List<String> users = client.getUsers(login);
            for (String user : users) {
                if (!Objects.equals(login, user)) {
                    MessagePane messagePane = new MessagePane(client, user);
                    if (!firstUser) {
                        chattingWith.setText(user);
                        firstUser = true;
                    }
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
