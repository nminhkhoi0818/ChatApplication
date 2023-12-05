package ChatClient;

public interface MessageListener {
    public void onMessage(String fromLogin, String msgBody);
}
