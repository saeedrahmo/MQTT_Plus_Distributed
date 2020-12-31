package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;

import java.io.IOException;
import java.net.*;

public class DiscoverySender extends Thread {

    private final String multicastAddress;
    private final int port = 4446;

    public DiscoverySender(String multicastAddress){
        this.multicastAddress = multicastAddress;
    }

    @Override
    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress group = InetAddress.getByName(multicastAddress);
            byte[] buf;

            String header = "MQTT+ Distributed Discovery Message";
            String payload = AdvertisementHandling.myHostname(JavaHTTPServer.local);
            buf = String.join("\n", header, payload).getBytes();

            DatagramPacket packet = new DatagramPacket(buf, buf.length, group, port);
            socket.send(packet);
            DiscoveryHandler.getInstance().setStartingTime(System.nanoTime());
            socket.close();

        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }catch (IOException ex){

        }

    }
}
