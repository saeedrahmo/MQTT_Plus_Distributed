package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class DiscoveryPacketHandler implements Runnable{

    private DatagramPacket packet;

    public DiscoveryPacketHandler(DatagramPacket packet){
        this.packet = packet;
    }

    private int checkHeader(String header){
        if(header.compareTo("MQTT+ Distributed Discovery Message") ==0){
            return 0;
        }else if(header.contains("MQTT+ Distributed RTT Message")){
            return 1;
        }else{
            return -1;
        }
    }


    @Override
    public void run() {
        synchronized (DiscoveryHandler.getInstance()) {
            String packetContent = new String(packet.getData(), 0, packet.getLength());
            String header = decodeHeader(packetContent);
            if (checkHeader(header) == 0) {
                if (!(JavaHTTPServer.getState().equals(ServerState.valueOf("DISCOVERY"))) && !DiscoveryHandler.getInstance().isMessageIDReceived(decodeMessageID(packetContent))) {
                    System.out.println("BUG");
                    JavaHTTPServer.setState(ServerState.valueOf("DISCOVERY"));
                    DiscoveryHandler.getInstance().clearDiscoveredAddresses();
                    synchronized (DiscoveryHandler.getInstance()) {
                        DiscoveryHandler.getInstance().notifyAll();
                    }
                }
                if (!DiscoveryHandler.getInstance().isProxyDiscovered(decodeProxyAddress(packetContent))) {
                    System.out.println(packetContent);
                    System.out.println(decodeRTTAddress(packetContent));
                    System.out.println(" ");
                    DiscoveryHandler.getInstance().insertDiscoveredAddress(decodeProxyAddress(packetContent), decodeBrokerAddress(packetContent));
                    DiscoveryHandler.getInstance().insertDiscoveredRTTAddress(decodeProxyAddress(packetContent), decodeRTTAddress(packetContent));
                    DiscoveryHandler.getInstance().insertDiscoveredSTPAddress(decodeProxyAddress(packetContent), decodeSTPAddress(packetContent));
                    DiscoveryHandler.getInstance().insertReceivedDiscoveryMessageID(decodeMessageID(packetContent));

                }
            }
        }
    }

    private String decodeProxyAddress(String packet){
        return packet.split("\\n")[2].split(" ")[2];
    }

    private String decodeBrokerAddress(String packet){
        return packet.split("\\n")[1].split(" ")[2];
    }

    private String decodeHeader(String packet){
        return packet.split("\\n")[0];
    }

    private String decodeRTTAddress(String packet){
        return packet.split("\\n")[3].split(" ")[3];
    }

    private String decodeSTPAddress(String packet) {
        return packet.split("\\n")[4].split(" ")[3];
    }

    private String decodeMessageID(String packet){
        return packet.split("\\n")[5].split(" ")[1];
    }

}
