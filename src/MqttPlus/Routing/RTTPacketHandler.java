package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;

import java.net.DatagramPacket;

public class RTTPacketHandler implements Runnable{

    private DatagramPacket packet;

    public RTTPacketHandler(DatagramPacket packet){
        this.packet = packet;
    }

    @Override
    public void run() {
        String packetContent = new String(packet.getData(), packet.getOffset(), packet.getLength());
        System.out.println("Packet received: " + packetContent);
        System.out.println(" ");
        if(packetContent.contains("Response")){
            synchronized (RTTHandler.getInstance()) {
                if (!(JavaHTTPServer.getState().equals(ServerState.valueOf("DISCOVERY"))) && !RTTMsgSender.isRestarted() && RTTHandler.getInstance().containsRequest(decodeRequestNumber(packetContent))) {
                    RTTHandler.getInstance().computeRTT(decodeDestination(packetContent));
                }
                RTTHandler.getInstance().removeRequestNumber(decodeRequestNumber(packetContent));
                RTTHandler.getInstance().notify();
            }
        }else if(packetContent.contains("Request")){
            RTTMsgSender sender = new RTTMsgSender(decodeDestination(packetContent), true, decodeRequestNumber(packetContent));
            sender.start();
        }
    }

    public String decodeDestination(String packet){
        return packet.split("\\n")[1];
    }

    public String decodeRequestNumber(String packet){
        String partialString =  packet.split(":")[1];
        return partialString.split("\\n")[0];
    }
}
