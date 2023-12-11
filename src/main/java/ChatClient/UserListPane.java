package ChatClient;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class UserListPane extends JPanel implements UserStatusListener {
    private final ChatClient client;
    private JList<String> userListUI;
    private DefaultListModel<String> userListModel;

    private ChatWindow chatWindow;

    public UserListPane(ChatClient client, ChatWindow chatWindow) throws IOException {
        this.client = client;
        this.client.addUserStatusListener(this);
        this.chatWindow = chatWindow;

        userListModel = new DefaultListModel<>();
        userListUI = new JList<>(userListModel);
        setLayout(new BorderLayout());
        add(new JScrollPane(userListUI), BorderLayout.CENTER);

        getAllUser();
        getAllGroups();

        userListUI.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    String login = userListUI.getSelectedValue();
                    chatWindow.showMessagePane(login);
                }
            }
        });
    }

    @Override
    public void online(String login) {
        userListModel.addElement(login);
    }

    @Override
    public void offline(String login) {
        userListModel.removeElement(login);
    }

    public void getAllUser() throws IOException {
        List<String> users = client.getUsers(client.getLogin());
        for (String user : users) {
            if (!user.equalsIgnoreCase(client.getLogin())) {
                userListModel.addElement(user);
            }
        }
    }

    public void getAllGroups() throws IOException {
        List<String> groups = client.getGroups(client.getLogin());
        for (int i = 0; i < groups.size(); i++) {
            if (i != 0) {
                userListModel.addElement(groups.get(i).split(" ")[1]);
            }
        }
    }

    public void addGroup(String groupName) {
        userListModel.addElement(groupName);
    }
}
