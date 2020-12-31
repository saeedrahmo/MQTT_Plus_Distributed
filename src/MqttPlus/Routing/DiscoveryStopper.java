package MqttPlus.Routing;

import java.io.IOException;
import java.net.InetAddress;
import java.util.TimerTask;

public class DiscoveryStopper extends TimerTask {

    private final DiscoveryReceiver receiver;
    private final DiscoverySender sender;

    public DiscoveryStopper(DiscoveryReceiver receiver, DiscoverySender sender) {
        this.receiver = receiver;
        this.sender = sender;

    }


    @Override
    public void run() {
        receiver.setIsRunning(false);
        sender.setIsRunning(true);
    }
}
