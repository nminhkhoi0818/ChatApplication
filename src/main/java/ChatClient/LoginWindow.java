package ChatClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class LoginWindow extends JFrame {
    private final ChatClient client;
    private static boolean userListCreated = false;
    JTextField loginField = new JTextField();
    JPasswordField passwordField = new JPasswordField();
    JButton loginButton = new JButton("Login");

    public LoginWindow() throws IOException {
        super("Login");

        this.client = new ChatClient("localhost", 8818);
        this.client.connect();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(loginField);
        p.add(passwordField);
        p.add(loginButton);

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    doLogin();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        getContentPane().add(p, BorderLayout.CENTER);

        pack();
        setVisible(true);
    }

    private void doLogin() throws IOException {
        String login = loginField.getText();
        String password = passwordField.getText();

        if (client.login(login, password)) {
            // Bring up the user list window
            UserListPane userListPane = new UserListPane(client);
            JFrame frame = new JFrame("User List");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 600);
            frame.getContentPane().add(userListPane, BorderLayout.CENTER);
            frame.setVisible(true);

            setVisible(false);

        } else {
            JOptionPane.showMessageDialog(this, "Invalid login/password");
        }
    }

    public static void main(String[] args) throws IOException {
        LoginWindow loginWindow = new LoginWindow();
        loginWindow.setVisible(true);

        LoginWindow loginWindow2 = new LoginWindow();
        loginWindow2.setVisible(true);

        LoginWindow loginWindow3 = new LoginWindow();
        loginWindow3.setVisible(true);
    }
}
