package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.TimeUnit;

public class DiscoverySender extends Thread {

    private final String multicastAddress;
    private final int port = 4446;
    private boolean isRunning;

    public DiscoverySender(String multicastAddress){
        this.multicastAddress = multicastAddress;
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
            buf = String.join("\n", header, payloadLineOne, payloadLineTwo).getBytes();

            DatagramPacket packet = new DatagramPacket(buf, buf.length, group, port);
            while(getIsRunning()) {
                socket.send(packet);
                TimeUnit.SECONDS.sleep(3);
            }


            socket.close();

        } catch (SocketException | UnknownHostException e) {
                e.printStackTrace();
        } catch (IOException ex) {

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public synchronized void setIsRunning(boolean value){
        isRunning = value;
    }

    public synchronized boolean getIsRunning(){
        return isRunning;
    }

}
