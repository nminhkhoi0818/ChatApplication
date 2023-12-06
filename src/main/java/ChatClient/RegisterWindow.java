package ChatClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

public class RegisterWindow extends JFrame {
    JLabel userLabel = new JLabel("Username");
    JLabel passwordLabel = new JLabel("Password");
    JLabel confirmPasswordLabel = new JLabel("Confirm Password");
    JTextField loginField = new JTextField();
    JPasswordField passwordField = new JPasswordField();
    JPasswordField confirmPasswordField = new JPasswordField();
    JButton registerButton = new JButton("Register");
    JButton loginButton = new JButton("Login");
    LoginWindow loginWindow;
    public RegisterWindow() throws IOException {
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if(handleRegister()) {
                        JOptionPane.showMessageDialog(RegisterWindow.this, "Create account successfully", "Notification", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(RegisterWindow.this, "Invalid username/password", "Notification", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RegisterWindow.this.setVisible(false);
                showLoginWindow();
            }
        });
        setLayout(new GridLayout(4, 2, 5, 5));
        add(userLabel);
        add(loginField);
        add(passwordLabel);
        add(passwordField);
        add(confirmPasswordLabel);
        add(confirmPasswordField);
        add(registerButton);
        add(loginButton);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Register");
        setSize(300, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private boolean handleRegister() throws IOException {
        String login = loginField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        if (!Objects.equals(login, "") && password.equals(confirmPassword)) {
            BufferedWriter writer = new BufferedWriter(new FileWriter("assets/auth.txt", true));
            writer.write(login + "`" + password);
            writer.newLine();
            writer.close();
            return true;
        }
        return false;
    }

    private void showLoginWindow() {
        if (loginWindow == null) {
            try {
                loginWindow = new LoginWindow();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new RegisterWindow();
    }
}
