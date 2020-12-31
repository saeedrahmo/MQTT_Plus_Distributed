package MqttPlus.Routing;

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
        }else if(header.compareTo("MQTT+ Distributed RTT computation Message") == 0){
            return 1;
        }else{
            return -1;
        }
    }

    private void sendRTTComputationMessage(DatagramPacket packet){
        try {
            DatagramSocket socket = new DatagramSocket(4445);
            InetAddress destinationAddress = packet.getAddress();
            int destinationPort = packet.getPort();
            String message = "MQTT+ Distributed RTT computation Message\n";
            DatagramPacket RTTPacket = new DatagramPacket(message.getBytes(), message.getBytes().length, destinationAddress, destinationPort);
            socket.send(RTTPacket);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        String packetContent = new String(packet.getData(), 0, packet.getLength());
        String header = packetContent.split("\\n")[0];
        if(checkHeader(header)==1){
            //TODO insert a table with the RTT
            long RTT = DiscoveryHandler.getInstance().getStartingTime() - System.nanoTime();
        }else if(checkHeader(header) == 0){
            DiscoveryHandler.getInstance().insertDiscoveredAddress(packetContent.split("\\n")[1]);
            sendRTTComputationMessage(packet);
        }


    }


}
