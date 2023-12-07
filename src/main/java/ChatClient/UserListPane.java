package ChatClient;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class UserListPane extends JPanel implements UserStatusListener {
    private final ChatClient client;
    private JList<String> userListUI;
    private DefaultListModel<String> userListModel;

    private ChatWindow chatWindow;

    public UserListPane(ChatClient client, ChatWindow chatWindow) {
        this.client = client;
        this.client.addUserStatusListener(this);
        this.chatWindow = chatWindow;

        userListModel = new DefaultListModel<>();
        userListUI = new JList<>(userListModel);
        setLayout(new BorderLayout());
        add(new JScrollPane(userListUI), BorderLayout.CENTER);

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
}
