package ChatClient;

public interface UserStatusListener {
    public void online(String login);
    public void offline(String login);
    public void addUser(String login, boolean group);
}
