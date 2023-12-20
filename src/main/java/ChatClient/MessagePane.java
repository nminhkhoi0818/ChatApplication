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

    private String[] members;

    public String[] getMembers() {
        return members;
    }

    private DefaultListModel<Message> listModel = new DefaultListModel<>();
    private JList<Message> messageList = new JList<>(listModel);
    private JTextField inputField = new JTextField();
    JButton fileButton;

    public String getLogin() {
        return login;
    }

    public MessagePane(ChatClient client, String login, String[] members) throws IOException {
        this.login = login;
        this.client = client;
        if (members != null && members.length > 0) {
            this.members = members;
        }
        client.addMessageListener(this);
        setLayout(new BorderLayout());
        add(new JScrollPane(messageList), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        fileButton = new JButton("File");
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        JButton refreshButton = new JButton("Refresh");
        buttonPanel.add(fileButton);
        buttonPanel.add(refreshButton);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);
        client.getMessageHistory(client.getLogin(), login, (members != null));
        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(MessagePane.this);

                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    try {
                        String clickableText = "<html>" + client.getLogin() + ": " + "<span style='color: blue; text-decoration: underline; cursor: pointer;'>" + selectedFile.getName() + "</span></html>";
                        listModel.addElement(new Message(0, clickableText));
                        client.sendFile(login, selectedFile);
                    } catch (Exception ex) {
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
                        if (members != null) {
                            client.msgGroup(login, text);
                        } else {
                            client.msg(login, text);
                        }
                         listModel.addElement(new Message(0, client.getLogin() + ": " + text));
                        inputField.setText("");
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listModel.clear();
                try {
                    client.getMessageHistory(client.getLogin(), login, (members != null));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });


        messageList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = messageList.locationToIndex(e.getPoint());
                    Message selected = listModel.getElementAt(index);
                    int startIndex = selected.getContent().indexOf("<span");
                    int endIndex = selected.getContent().indexOf("</span>", startIndex);

                    if (startIndex != -1 && endIndex != -1) {
                        String spanContent = selected.getContent().substring(startIndex, endIndex);
                        int closingBracketIndex = spanContent.indexOf(">");
                        if (closingBracketIndex != -1) {
                            String fileName = spanContent.substring(closingBracketIndex + 1);
                            JFileChooser fileChooser = new JFileChooser();
                            fileChooser.setSelectedFile(new File(fileName));
                            fileChooser.setVisible(true);
                            int result = fileChooser.showOpenDialog(MessagePane.this);

                            if (result == JFileChooser.APPROVE_OPTION) {
                                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                                try {
                                    client.requestDownloadFile(login, fileName, filePath);
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                            }
                        }
                    }
                }
                if (e.getButton() == MouseEvent.BUTTON3) {
                    int index = messageList.locationToIndex(e.getPoint());
                    Message selectedMessage = listModel.getElementAt(index);

                    int option = JOptionPane.showConfirmDialog(
                            MessagePane.this,
                            "Are you sure you want to delete this message?",
                            "Delete Confirmation",
                            JOptionPane.YES_NO_OPTION);

                    if (option == JOptionPane.YES_OPTION && selectedMessage.getId() != 0) {
                        listModel.remove(index);
                        try {
                            if (members != null) {
                                client.deleteGroupMessage(selectedMessage.getId());
                            } else {
                                client.deleteMessage(selectedMessage.getId());
                            }
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onMessage(String fromLogin, String msgBody, boolean history, boolean file, String sender) throws IOException {
        if (history) {
            if (login.equalsIgnoreCase(fromLogin)) {
                String[] msg =  msgBody.split(" ", 3);
                Integer messageId = Integer.valueOf(msg[0]);
                String line = msg[1] + ": " + msg[2];

                SwingUtilities.invokeLater(() -> {
                    listModel.addElement(new Message(messageId, line));
                    messageList.ensureIndexIsVisible(listModel.size() - 1);
                });
            }
        }
        else {
            if (login.equalsIgnoreCase(fromLogin)) {
                if (sender != null && sender.equalsIgnoreCase(client.getLogin())) {
                    return;
                }
                String line;
                line = Objects.requireNonNullElse(sender, fromLogin) + ": " + msgBody;
                if (file) {
                    String clickableText = "<html>" + fromLogin + ": " + "<span style='color: blue; text-decoration: underline; cursor: pointer;'>" + msgBody + "</span></html>";
                    SwingUtilities.invokeLater(() -> {
                        listModel.addElement(new Message(0, clickableText));
                        messageList.ensureIndexIsVisible(listModel.size() - 1);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        listModel.addElement(new Message(0, line));
                        messageList.ensureIndexIsVisible(listModel.size() - 1);
                    });
                }
            }
        }
    }
}
