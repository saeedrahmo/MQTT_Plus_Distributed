package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;

import java.io.IOException;
import java.net.*;

public class RTTMsgSender extends Thread{

    private String destIp;
    private String destTableAccess;
    private final boolean response; // true for response, false for fist message
    private static boolean restarted;
    private String requestNumber;
    private Thread stateObserver;

    public RTTMsgSender(String dest, boolean response, String requestNumber){
        RTTMsgSender instance = this;
        stateObserver = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean run = true;
                while(run){
                    if(JavaHTTPServer.getState().equals(ServerState.valueOf("DISCOVERY"))){
                        instance.interrupt();
                        run = false;
                    }
                }
            }
        });
        stateObserver.start();
        destTableAccess = dest;
        this.response = response;
        restarted = false;
        this.requestNumber = requestNumber;
    }

    @Override
    public void run() {

        synchronized (DiscoveryHandler.getInstance()) {
            try {
                System.out.println("Dentro sender");
                DatagramSocket socket = new DatagramSocket();
                byte[] buf;
                String header;
                String body;
                if (!response) {
                    if (isRestarted() || DiscoveryHandler.getInstance().isRestarted()) {
                        setRestarted(false);
                        return;
                    }
                    header = "MQTT+ Distributed RTT Message Request:" + requestNumber + "\n";
                    body = AdvertisementHandling.myHostname(JavaHTTPServer.local).split(":")[0] + ":" + JavaHTTPServer.PORT + "\n";
                } else {
                    if (isRestarted() || DiscoveryHandler.getInstance().isRestarted()) {
                        return;
                    }
                    header = "MQTT+ Distributed RTT Message Response:" + requestNumber + "\n";
                    body = AdvertisementHandling.myHostname(JavaHTTPServer.local).split(":")[0] + ":" + JavaHTTPServer.PORT + "\n";
                }
                String content = header + body;
                buf = content.getBytes();
                String destination = DiscoveryHandler.getInstance().getRTTAddress(destTableAccess);
                if(destination == null){
                    return;
                }
                String hostname = destination.split(":")[0];
                String port = destination.split(":")[1];
                InetAddress destAddress = InetAddress.getByName(hostname);
                DatagramPacket packet = new DatagramPacket(buf, buf.length, destAddress, new Integer(port));
                RTTHandler.getInstance().insertExpirationTimer(requestNumber);
                socket.send(packet);
                System.out.println("Sent: " + content + "\n" + "Destination: " + destination);
                System.out.println(" ");
                if (!response) {
                    RTTHandler.getInstance().addStartingTime(destTableAccess);
                }
                socket.close();

            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static synchronized boolean isRestarted(){
        return restarted;
    }

    public static synchronized void setRestarted(boolean value){
        restarted = value;
    }
}
