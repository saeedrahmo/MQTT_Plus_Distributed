package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;

import java.io.IOException;
import java.net.*;

public class STPSender extends Thread{

    private String dest;
    private String root;
    private boolean sendFinishRoot;


    public STPSender(String dest, boolean sendFinishRoot){
        this.dest = dest;
        this.sendFinishRoot = sendFinishRoot;
        root = STPHandler.getInstance().getRoot();
    }

    @Override
    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket();
            byte[] buf;
            if(!sendFinishRoot) {
                String root = new String();
                String pathCost = new String();
                if(!STPHandler.getInstance().getState().equals(STPState.valueOf("ROOT"))){
                    root = "Root: " + STPHandler.getInstance().getRoot();
                    pathCost = "P: " + STPHandler.getInstance().getPathCost();
                }else{
                    root = "Root: " + DiscoveryHandler.getInstance().getSelfAddress();
                    pathCost = "P: 0";
                }
                String header = "MQTT+ STP Message";
                String M = "M: " + STPHandler.getInstance().getM().toString();
                String L = "L: " + STPHandler.getInstance().getL().toString();

                String source = "Source: " + DiscoveryHandler.getInstance().getSelfAddress();
                String packetContent = String.join("\n", header, root, M, L, pathCost, source);
                buf = packetContent.getBytes();
            }else{
                String header = "MQTT+ STP root selection completed";
                String source ="Source: " + DiscoveryHandler.getInstance().getSelfAddress();
                String packetContent = String.join("\n", header, source);
                buf = packetContent.getBytes();
            }
            String destination = DiscoveryHandler.getInstance().getSTPAddress(dest);
            String hostname = destination.split(":")[0];
            String port = destination.split(":")[1];
            InetAddress destAddress = InetAddress.getByName(hostname);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, destAddress, new Integer(port));
            socket.send(packet);
            System.out.println("STP PACKET SENT: " + new String(packet.getData(), packet.getOffset(), packet.getLength()));
            if(!sendFinishRoot && STPHandler.getInstance().getState().equals(STPState.valueOf("ROOT"))) {
                synchronized (STPHandler.getInstance()) {
                    STPHandler.getInstance().insertLocalRootMessageDestination(dest);
                    if (STPHandler.getInstance().isRootFinished()) {
                        STPHandler.getInstance().sendFinishRootPhase();
                    }
                }
            }

            if(sendFinishRoot) {
                STPHandler.getInstance().insertRootFinishedMessageDestination(dest);
                if (STPHandler.getInstance().canPassToNormal()) {
                    STPHandler.getInstance().setState(STPState.valueOf("NORMAL"));
                    synchronized (STPHandler.getInstance()) {
                        STPHandler.getInstance().notifyAll();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
