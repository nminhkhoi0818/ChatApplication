package ChatClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class MessagePane extends JPanel implements MessageListener {
    private final ChatClient client;
    private final String login;
    private DefaultListModel<String> listModel = new DefaultListModel<>();
    private JList<String> messageList = new JList<>(listModel);
    private JTextField inputField = new JTextField();
    JButton sendButton;
    JButton fileButton;

    public String getLogin() {
        return login;
    }

    public MessagePane(ChatClient client, String login) throws IOException {
        this.login = login;
        this.client = client;
        client.addMessageListener(this);
        setLayout(new BorderLayout());
        add(new JScrollPane(messageList), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        sendButton = new JButton("Send");
        fileButton = new JButton("File");
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        buttonPanel.add(sendButton);
        buttonPanel.add(fileButton);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);
        client.getMessageHistory(client.getLogin(), login);

        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String text = inputField.getText();
                    if (!text.isEmpty()) {
                        client.msg(login, text);
                        listModel.addElement(client.getLogin() + ": " + text);
                        inputField.setText("");
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    @Override
    public void onMessage(String fromLogin, String msgBody, boolean history) {
        // Check if send to the current user
        if (history) {
            if (login.equalsIgnoreCase(fromLogin)) {
                String[] msg =  msgBody.split(" ", 2);
                String line = msg[0] + ": " + msg[1];
                SwingUtilities.invokeLater(() -> {
                    listModel.addElement(line);
                    messageList.ensureIndexIsVisible(listModel.size() - 1);
                });
            }
        }
        else {
            if (login.equalsIgnoreCase(fromLogin)) {
                String line = fromLogin + ": " + msgBody;
                SwingUtilities.invokeLater(() -> {
                    listModel.addElement(line);
                    messageList.ensureIndexIsVisible(listModel.size() - 1);
                });
            }
        }
    }
}
