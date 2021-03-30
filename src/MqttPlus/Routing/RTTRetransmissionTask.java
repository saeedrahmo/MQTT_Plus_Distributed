package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;

import java.util.TimerTask;

public class RTTRetransmissionTask extends TimerTask {

    private String requestNumber;

    public RTTRetransmissionTask(String requestNumber){
        this.requestNumber = requestNumber;
    }

    @Override
    public void run() {
        synchronized (RTTHandler.getInstance()) {
            if (RTTHandler.getInstance().getRetransmissionCount(requestNumber) == 2 && !JavaHTTPServer.getState().equals(ServerState.valueOf("DISCOVERY"))) {
                DiscoveryHandler.getInstance().clearDiscoveredAddresses();
                JavaHTTPServer.setState(ServerState.valueOf("DISCOVERY"));
                synchronized (DiscoveryHandler.getInstance()) {
                    DiscoveryHandler.getInstance().notifyAll();
                }
            } else {
                RTTHandler.getInstance().removeExpirationTimer(requestNumber);
                RTTHandler.getInstance().incrementRetransmissionCount(requestNumber);
                new RTTMsgSender(RTTHandler.getInstance().getRequestDestination(requestNumber), false, requestNumber).start();
            }
        }

    }
}
