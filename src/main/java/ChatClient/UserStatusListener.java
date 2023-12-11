package ChatClient;

import java.util.List;

public interface UserStatusListener {
    public void online(String login);
    public void offline(String login);
    public void addUser(String login);
}
