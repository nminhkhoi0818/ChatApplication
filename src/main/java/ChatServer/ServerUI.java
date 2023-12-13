package ChatServer;

import javax.swing.*;
import java.awt.*;

public class ServerUI {
    private JFrame frame;
    private JTextArea textArea;

    public ServerUI() {
        frame = new JFrame("Server");
        textArea = new JTextArea();

        textArea.setEditable(false);
        frame.add(new JScrollPane(textArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public void showInfo(String msg) {
        textArea.append(msg + "\n");
    }
}
