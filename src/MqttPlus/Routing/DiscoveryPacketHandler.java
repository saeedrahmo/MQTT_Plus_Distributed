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
        String packetContent = new String(packet.getData(), 0, packet.getLength());
        String header = decodeHeader(packetContent);
        if(checkHeader(header) == 0){
            if(!DiscoveryHandler.getInstance().isProxyDiscovered(decodeProxyAddress(packetContent))){
                if(!(JavaHTTPServer.getState().equals(ServerState.valueOf("DISCOVERY")))){
                    JavaHTTPServer.setState(ServerState.DISCOVERY);
                    DiscoveryHandler.getInstance().clearDiscoveredAddresses();
                    synchronized (DiscoveryHandler.getInstance()){
                        DiscoveryHandler.getInstance().notify();
                    }
                }
                System.out.println(packetContent);
                System.out.println(decodeRTTAddress(packetContent));
                System.out.println(" ");
                DiscoveryHandler.getInstance().insertDiscoveredAddress(decodeProxyAddress(packetContent), decodeBrokerAddress(packetContent));
                DiscoveryHandler.getInstance().insertDiscoveredRTTAddress(decodeProxyAddress(packetContent), decodeRTTAddress(packetContent));

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

}
