package ChatClient;

import java.io.IOException;

public interface MessageListener {
    public void onMessage(String fromLogin, String msgBody, boolean history, boolean file, String sender) throws IOException;
}
