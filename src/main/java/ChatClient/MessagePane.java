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

    private DefaultListModel<String> listModel = new DefaultListModel<>();
    private JList<String> messageList = new JList<>(listModel);
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
        buttonPanel.add(fileButton);
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
                        listModel.addElement(clickableText);
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
                        if (members != null) {
                            client.msgGroup(login, text);
                        } else {
                            client.msg(login, text);
                        }
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
                if (e.getClickCount() == 2) {
                    int index = messageList.locationToIndex(e.getPoint());
                    String selected = listModel.getElementAt(index);
                    int startIndex = selected.indexOf("<span");
                    int endIndex = selected.indexOf("</span>", startIndex);

                    if (startIndex != -1 && endIndex != -1) {
                        String spanContent = selected.substring(startIndex, endIndex);
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
            }
        });
    }

    @Override
    public void onMessage(String fromLogin, String msgBody, boolean history, boolean file, String sender) throws IOException {
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
                if (sender != null && sender.equalsIgnoreCase(client.getLogin())) {
                    return;
                }
                String line;
                line = Objects.requireNonNullElse(sender, fromLogin) + ": " + msgBody;
                if (file) {
                    String clickableText = "<html>" + fromLogin + ": " + "<span style='color: blue; text-decoration: underline; cursor: pointer;'>" + msgBody + "</span></html>";
                    SwingUtilities.invokeLater(() -> {
                        listModel.addElement(clickableText);
                        messageList.ensureIndexIsVisible(listModel.size() - 1);
                    });
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
