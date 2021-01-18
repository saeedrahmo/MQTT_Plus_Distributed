package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;

import java.net.DatagramPacket;

public class STPacketHandler implements Runnable{

    private final DatagramPacket packet;
    private boolean rootFinishedMessage;

    public STPacketHandler(DatagramPacket packet){
        this.packet = packet;
        rootFinishedMessage = false;
    }

    //TODO: ask how to compute C = alpha*L + beta*M, ask what are the typical value of alpha and beta

    @Override
    public void run() {
        String packetContent = new String(packet.getData(), packet.getOffset(), packet.getLength());

        while(!JavaHTTPServer.getState().equals(ServerState.valueOf("STP"))){
            synchronized (STPHandler.getInstance()){
                try {
                    STPHandler.getInstance().wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if(decodeHeader(packetContent).equals("MQTT+ STP root selection completed")){
            rootFinishedMessage = true;
        }

        if (STPHandler.getInstance().isRootFinished() && !rootFinishedMessage) {
            while (STPHandler.getInstance().getState().equals(STPState.valueOf("ROOT"))) {
                synchronized (STPHandler.getInstance()) {
                    try {
                        STPHandler.getInstance().wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        System.out.println("STP packet content: " +  packetContent);

        if((STPHandler.getInstance().getState().equals(STPState.valueOf("ROOT")) || STPHandler.getInstance().getState().equals(STPState.valueOf("RESTARTED"))) && !rootFinishedMessage){
            String root = decodeRoot(packetContent);
            Double L = new Double(decodeL(packetContent));
            Double M = new Double(decodeM(packetContent));
            Long pathCost = new Long(decodePathCost(packetContent));
            String source = decodeSource(packetContent);
            Double C = STPHandler.getInstance().getAlpha()*L + STPHandler.getInstance().getBeta()*M;
            Double localC = STPHandler.getInstance().getAlpha()*STPHandler.getInstance().getRootL() + STPHandler.getInstance().getBeta()*STPHandler.getInstance().getAlpha()*STPHandler.getInstance().getRootM();
            if(C > localC){
                setNewRoot(root, M, L);
            }else if(C.equals(localC)){
                if(STPHandler.getInstance().getRoot().compareTo(root) > 0){
                    setNewRoot(root, M, L);
                }
            }
            synchronized (STPHandler.getInstance()) {
                STPHandler.getInstance().insertRootRequestSource(source);
                if (STPHandler.getInstance().isRootFinished()) {
                    STPHandler.getInstance().sendFinishRootPhase();
                    STPHandler.getInstance().setOriginalRoot(STPHandler.getInstance().getRoot());
                }
            }
        }else if((STPHandler.getInstance().getState().equals(STPState.valueOf("ROOT")) || STPHandler.getInstance().getState().equals(STPState.valueOf("RESTARTED"))) && rootFinishedMessage){
            synchronized (STPHandler.getInstance()) {
                String source = decodeSource(packetContent);
                STPHandler.getInstance().insertRootFinishedMessageSource(source);
                if (STPHandler.getInstance().canPassToNormal()) {
                    STPHandler.getInstance().setState(STPState.valueOf("NORMAL"));
                    STPHandler.getInstance().notifyAll();
                }
            }
        }else if(STPHandler.getInstance().getState().equals(STPState.valueOf("NORMAL"))){
            String source = decodeSource(packetContent);
            String root = decodeRoot(packetContent);
            Long pathCost = new Long(decodePathCost(packetContent));
            if(!STPHandler.getInstance().getRoot().equals(DiscoveryHandler.getInstance().getSelfAddress())) {
                if (!root.equals(DiscoveryHandler.getInstance().getSelfAddress())) {
                    synchronized (STPHandler.getInstance()) {
                        if (STPHandler.getInstance().getPathCost().compareTo(pathCost + RTTHandler.getInstance().getRTT(source)) > 0) {
                            STPHandler.getInstance().setRoot(source);
                            STPHandler.getInstance().setPathCost(pathCost + RTTHandler.getInstance().getRTT(source));
                            for (String proxy : DiscoveryHandler.getInstance().getProxies()) {
                                new STPSender(proxy, false).start();
                            }
                        }
                        if (STPHandler.getInstance().containsChild(source)) {
                            STPHandler.getInstance().removeChild(source);
                        }

                    }
                } else {
                    STPHandler.getInstance().insertChild(source);
                }
            }else{
                if(root.equals(DiscoveryHandler.getInstance().getSelfAddress())){
                    STPHandler.getInstance().insertChild(source);
                }else if(STPHandler.getInstance().containsChild(source) && !root.equals(DiscoveryHandler.getInstance().getSelfAddress())){
                    STPHandler.getInstance().removeChild(source);
                }
            }
        }

    }

    private String decodeHeader(String packetContent){
        return packetContent.split("\\n")[0];
    }

    private String decodeRoot(String packetContent){
        return packetContent.split("\\n")[1].split(" ")[1];
    }

    private String decodeM(String packetContent){
        return packetContent.split("\\n")[2].split(" ")[1];
    }

    private String decodeL(String packetContent){
        return packetContent.split("\\n")[3].split(" ")[1];
    }

    private String decodePathCost(String packetContent){
        return packetContent.split("\\n")[4].split(" ")[1];
    }

    private String decodeSource(String packetContent){
        if(!rootFinishedMessage)
            return packetContent.split("\\n")[5].split( " ")[1];
        else
            return packetContent.split("\\n")[1].split(" ")[1];
    }

    private void setNewRoot(String root,Double M, Double L){
        STPHandler.getInstance().setRoot(root);
        STPHandler.getInstance().setRootL(L);
        STPHandler.getInstance().setRootM(M);
        System.out.println("Setting the new root, RTT:" + RTTHandler.getInstance().getRTT(root));
        STPHandler.getInstance().setPathCost(RTTHandler.getInstance().getRTT(root));
    }
}
