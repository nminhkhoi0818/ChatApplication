package ChatClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

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
        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(MessagePane.this);

                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    try {
                        listModel.addElement(client.getLogin() + ": " + selectedFile.getName());
                        client.sendFile(login, selectedFile);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
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

        messageList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = messageList.locationToIndex(e.getPoint());
                String selected = listModel.getElementAt(index);
            }
        });
    }

    @Override
    public void onMessage(String fromLogin, String msgBody, boolean history, boolean file) throws IOException {
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
                if (file) {
                    if (client.checkExistFile(msgBody)) {
                        String clickableText = "<html>" + fromLogin + ": " + "<span style='color: blue; text-decoration: underline; cursor: pointer;'>" + msgBody + "</span></html>";
                        SwingUtilities.invokeLater(() -> {
                            listModel.addElement(clickableText);
                            messageList.ensureIndexIsVisible(listModel.size() - 1);
                        });
                    }
                } else {
                    SwingUtilities.invokeLater(() -> {
                        listModel.addElement(line);
                        messageList.ensureIndexIsVisible(listModel.size() - 1);
                    });
                }
            }
        }
    }
}
