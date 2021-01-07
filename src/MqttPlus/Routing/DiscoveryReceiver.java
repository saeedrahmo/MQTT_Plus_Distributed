package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class DiscoveryReceiver extends Thread{

    private final int port = 4446;
    private final String multicastAddress = "230.0.0.1";
    private MulticastSocket socket;
    private boolean isRunning;
    private static DiscoveryReceiver instance = null;


    private DiscoveryReceiver() {
        this.isRunning = true;

        try {
            this.socket = new MulticastSocket(port);
        } catch (IOException ex) {

        }
    }

    public static DiscoveryReceiver getInstance(){
        if(instance == null){
            instance = new DiscoveryReceiver();
        }
        return instance;
    }

   /* public String getMulticastAddress(){
        return multicastAddress;
    }

    public MulticastSocket getSocket(){
        return socket;
    }*/

    public synchronized boolean getIsRunning(){
        return isRunning;
    }


    @Override
    public void run() {
        try {
            InetAddress group = InetAddress.getByName(multicastAddress);
            socket.joinGroup(group);
            while(getIsRunning()){
                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                new Thread(new DiscoveryPacketHandler(packet)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    public synchronized void finish(){
        socket.close();
        isRunning = false;
    }
}
