package ChatClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatWindow extends JFrame implements ChatWindowListener {
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
    ArrayList<String> messagePanesList = new ArrayList<>();

    ChatWindow(String login, ChatClient client) throws IOException {
        this.client = client;
        this.login = login;
        this.client.addChatWindowListener(this);
        // Sidebar
        sideBar = new JPanel(new BorderLayout());
        userInfo = new JLabel("<html>Username: " + login + "<br>Port: " + client.getServerPort()+ "</html>");
        userListPane = new UserListPane(client, this);

        logoffButton = new JButton("Log Off");
        createGroupButton = new JButton("Create Group");
        JPanel buttonSideBarPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        buttonSideBarPanel.add(createGroupButton);
        buttonSideBarPanel.add(logoffButton);
        client.getUsers(login);
        createGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog chooseUserDialog = new JDialog();
                JPanel chooseUserContent = new JPanel();
                chooseUserContent.setLayout(new BorderLayout());

                JPanel userListPanel = new JPanel();
                userListPanel.setLayout(new BorderLayout());
                DefaultListModel<String> listModel = new DefaultListModel<>();

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

        messagePanes = new ArrayList<MessagePane>();
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        mainContent.add(cardPanel, BorderLayout.CENTER);

        mainContent.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        mainContent.add(chattingWith, BorderLayout.NORTH);

        setLayout(new BorderLayout());
        add(sideBar, BorderLayout.WEST);
        add(mainContent, BorderLayout.CENTER);

        sideBar.setPreferredSize(new Dimension(150, getHeight()));
        mainContent.setPreferredSize(new Dimension(getWidth() - 150, getHeight()));

        this.setSize(600, 500);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    public void showMessagePane(String login) {
        for (MessagePane messagePane : messagePanes) {
            if (login.equals(messagePane.getLogin())) {
                String[] members = messagePane.getMembers();
                if (members != null && members.length > 0) {
                    StringBuilder line = new StringBuilder("Members:");
                    for (String member : members) {
                        line.append(" ").append(member);
                    }
                    System.out.println(line);
                    chattingWith.setText(String.valueOf(line));
                } else {
                    chattingWith.setText(login);
                }
                cardLayout.show(cardPanel, login);
            }
        }
    }

    @Override
    public void addUser(String user, String[] members) {
        if (!user.equalsIgnoreCase(login) && !messagePanesList.contains(user)) {
            messagePanesList.add(user);
            try {
                MessagePane messagePane = new MessagePane(client, user, members);
                if (messagePanesList.size() == 1) {
                    chattingWith.setText(user);
                }
                messagePanes.add(messagePane);
                cardPanel.add(messagePane, user);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
