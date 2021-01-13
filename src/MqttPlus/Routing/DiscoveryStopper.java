package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;

import java.io.IOException;
import java.net.*;
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
        sender.finish();
        JavaHTTPServer.setState(ServerState.RTT);
        if(!RTTHandler.isStarted()){
            new Thread(RTTHandler.getInstance()).start();
        }else{
            RTTHandler.getInstance().restartHandler();
        }
    }

}
