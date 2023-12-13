package ChatClient;
import com.sun.source.doctree.SeeTree;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class UserListPane extends JPanel implements UserStatusListener {
    private final ChatClient client;
    private JList<String> userListUI;

    private Set<String> setUser = new HashSet<>();
    private DefaultListModel<String> userListModel;
    private ChatWindow chatWindow;
    private List<String> onlineUsers;

    public UserListPane(ChatClient client, ChatWindow chatWindow) throws IOException {
        this.client = client;
        this.client.addUserStatusListener(this);
        this.chatWindow = chatWindow;

        userListModel = new DefaultListModel<>();
        userListUI = new JList<>(userListModel);
        userListUI.setCellRenderer(new UserListCellRenderer());
        setLayout(new BorderLayout());
        add(new JScrollPane(userListUI), BorderLayout.CENTER);

        getAllUser();
        getAllGroups();

        onlineUsers = new ArrayList<>();

        userListUI.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() > 1) {
                String login = userListUI.getSelectedValue();
                chatWindow.showMessagePane(login.split(" ")[0]);
            }
            }
        });
    }

    @Override
    public void online(String login) {
        if (!setUser.contains(login) && !onlineUsers.contains(login)) {
            onlineUsers.add(login);
        }
        updateUserStatus(login, true);
    }

    @Override
    public void offline(String login) {
        updateUserStatus(login, false);
    }

    private void updateUserStatus(String login, boolean online) {
        int index = userListModel.indexOf(login);
        if (index != -1) {
            userListModel.setElementAt(login + (online ? " (Online)" : " (Offline)"), index);
        }
    }

    @Override
    public void addUser(String login) {
        if (!login.equalsIgnoreCase(client.getLogin()) && !userListModel.contains(login) && !setUser.contains(login)) {
            setUser.add(login);
            userListModel.addElement(login);
        }
        if (!onlineUsers.isEmpty() && onlineUsers.contains(login)) {
            updateUserStatus(login, true);
        }
    }

    public void getAllUser() throws IOException {
        client.getUsers(client.getLogin());
    }

    public void getAllGroups() throws IOException {
        client.getGroups(client.getLogin());
    }

    public void addGroup(String groupName) {
        userListModel.addElement(groupName);
    }

    private class UserListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String login = (String) value;
            if (login.contains("(Online)")) {
                renderer.setForeground(Color.GREEN);
            } else if (login.contains("(Offline)")) {
                renderer.setForeground(Color.RED);
            }
            return renderer;
        }
    }
}
