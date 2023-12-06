package ChatClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class LoginWindow extends JFrame {
    private final ChatClient client;
    private static boolean userListCreated = false;

    JLabel userLabel = new JLabel("Username");
    JLabel passwordLabel = new JLabel("Password");
    JTextField loginField = new JTextField();
    JPasswordField passwordField = new JPasswordField();
    JButton loginButton = new JButton("Login");
    JButton registerButton = new JButton("Register");
    RegisterWindow registerWindow;

    public LoginWindow() throws IOException {
        this.client = new ChatClient("localhost", 8818);
        this.client.connect();

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

        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LoginWindow.this.setVisible(false);
                if (registerWindow == null) {
                    try {
                        registerWindow = new RegisterWindow();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        setLayout(new GridLayout(3, 2, 5, 5));
        add(userLabel);
        add(loginField);
        add(passwordLabel);
        add(passwordField);
        add(loginButton);
        add(registerButton);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Login");
        setSize(300, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void doLogin() throws IOException {
        String login = loginField.getText();
        String password = passwordField.getText();

        if (client.login(login, password)) {
            // Navigate to chat window
            ChatWindow chatWindow = new ChatWindow();
            setVisible(false);
        } else {
            JOptionPane.showMessageDialog(this, "Invalid login/password");
        }
    }

    public static void main(String[] args) throws IOException {
        LoginWindow loginWindow = new LoginWindow();
        loginWindow.setVisible(true);
//
//        LoginWindow loginWindow2 = new LoginWindow();
//        loginWindow2.setVisible(true);
    }
}
