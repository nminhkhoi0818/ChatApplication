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
        handleMessageHistory();
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

        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String text = inputField.getText();
                    client.msg(login, text);
                    listModel.addElement("You: " + text);
                    inputField.setText("");
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    private void handleMessageHistory() throws IOException {
        client.getMessageHistory(client.getLogin(), login);
    }


    @Override
    public void onMessage(String fromLogin, String msgBody) {
        // Check if send to the current user
        if (login.equalsIgnoreCase(fromLogin)) {
            String line = fromLogin + ": " + msgBody;
            System.out.println();
            listModel.addElement(line);
        }
    }
}
