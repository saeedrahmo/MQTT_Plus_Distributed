package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;

import java.io.IOException;
import java.net.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DiscoverySender extends Thread {

    private final int port = 4446;
    private boolean isRunning;
    private final String multicastAddress = "230.0.0.1";

    public DiscoverySender(){
        this.isRunning = true;
    }

    @Override
    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress group = InetAddress.getByName(multicastAddress);
            byte[] buf;

            String header = "MQTT+ Distributed Discovery Message";
            String payloadLineOne = "Broker Address: " + AdvertisementHandling.myHostname(JavaHTTPServer.local);
            String payloadLineTwo = "Proxy Address: " + AdvertisementHandling.myHostname(JavaHTTPServer.local).split(":")[0] + ":" + JavaHTTPServer.PORT;
            String payloadLineThree = "RTT listening on: " + AdvertisementHandling.myHostname(JavaHTTPServer.local).split(":")[0] + ":" + DiscoveryHandler.getInstance().getRTTPort();
            String payloadLineFour = "STP listening on: " + AdvertisementHandling.myHostname(JavaHTTPServer.local).split(":")[0] + ":" + DiscoveryHandler.getInstance().getSTPort();
            String id = UUID.randomUUID().toString() + DiscoveryHandler.getInstance().getSelfAddress();
            while(DiscoveryHandler.getInstance().isIDPresent(id)){
                id = UUID.randomUUID().toString() + DiscoveryHandler.getInstance().getSelfAddress();
            }
            DiscoveryHandler.getInstance().insertDiscoveryMessageID(id);
            String payloadLineFive = "ID: " + id;
            buf = String.join("\n", header, payloadLineOne, payloadLineTwo, payloadLineThree, payloadLineFour, payloadLineFive).getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, group, port);

            while(getIsRunning()) {
                socket.send(packet);
            }

            socket.close();

        } catch (SocketException | UnknownHostException e) {
                e.printStackTrace();
        } catch (IOException ex) {

        }

    }

    public synchronized void finish(){
        isRunning = false;
    }

    public synchronized boolean getIsRunning(){
        return isRunning;
    }

}
