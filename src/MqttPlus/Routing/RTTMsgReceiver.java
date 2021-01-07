package MqttPlus.Routing;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class RTTMsgReceiver extends Thread{

    private DatagramSocket socket;
    private byte[] buf;
    public static RTTMsgReceiver instance = null;
    public boolean isRunning;

    private RTTMsgReceiver(){
        socket = DiscoveryHandler.getInstance().getRTTSocket();;
        isRunning = true;

    }

    public static RTTMsgReceiver getInstance(){
        if(instance == null){
            instance = new RTTMsgReceiver();
        }
        return instance;
    }

    @Override
    public void run() {
        System.out.println("RTT receiver starting, listening on port: " + socket.getLocalPort());
        while(getIsRunning()){
            buf = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
                (new Thread(new RTTPacketHandler(packet))).start();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public synchronized void finish(){
        if(socket!=null)
            socket.close();
        setIsRunning(false);
    }

    public synchronized void setIsRunning(boolean value){
        isRunning = value;
    }

    public synchronized boolean getIsRunning(){
        return isRunning;
    }


}
