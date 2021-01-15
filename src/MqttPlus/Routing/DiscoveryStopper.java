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
        if(!RTTHandler.isStarted()){
            JavaHTTPServer.setState(ServerState.valueOf("RTT"));
            new Thread(RTTHandler.getInstance()).start();
            synchronized (RTTHandler.getInstance()){
                RTTHandler.getInstance().notifyAll();
            }
        }else{
            RTTHandler.getInstance().restartHandler();
        }
    }

}
