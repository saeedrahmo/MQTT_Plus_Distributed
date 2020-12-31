package MqttPlus.Routing;

import java.io.IOException;
import java.net.InetAddress;
import java.util.TimerTask;

public class DiscoveryStopper extends TimerTask {

    private final DiscoveryReceiver receiver;

    public DiscoveryStopper(DiscoveryReceiver receiver) {
        this.receiver = receiver;
    }


    @Override
    public void run() {
        receiver.setIsRunning(false);
    }
}
