package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Timer;

public class STPReceiver extends Thread{

    private final DatagramSocket socket = DiscoveryHandler.getInstance().getSTPSocket();
    private boolean isRunning;
    private byte[] buf;
    private Timer timer;

    private static STPReceiver instance = null;

    private STPReceiver(){
       super();
       isRunning = true;
    }

    public static STPReceiver getInstance(){
        if(instance==null){
            instance = new STPReceiver();
        }
        return instance;
    }


    @Override
    public void run() {

        while(getIsRunning()){
            buf = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
                if(JavaHTTPServer.getState().equals(ServerState.valueOf("NORMAL"))){
                    JavaHTTPServer.setState(ServerState.valueOf("STP"));
                    STPHandler.getInstance().restartProtocol();
                }
                if(STPHandler.getInstance().getState().equals(STPState.valueOf("NORMAL"))) {
                    STPHandler.getInstance().cancelTimer();
                    STPHandler.getInstance().scheduleEndTimer();
                }
                (new Thread(new STPacketHandler(packet))).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    public synchronized boolean getIsRunning(){
        return isRunning;
    }

    public synchronized void setIsRunning(boolean value){
        isRunning = value;
    }


}
