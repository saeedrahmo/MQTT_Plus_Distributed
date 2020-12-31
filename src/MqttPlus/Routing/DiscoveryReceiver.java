package MqttPlus.Routing;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class DiscoveryReceiver extends Thread{

    private String multicastAddress;
    private final int port = 4446;
    private byte[] buf;
    private MulticastSocket socket;
    private boolean isRunning;

    public DiscoveryReceiver(String multicastAddress)  {
        this.multicastAddress = multicastAddress;
        buf = new byte[256];
        this.isRunning = true;

        try {
            this.socket = new MulticastSocket(port);
        }catch (IOException ex){

        }
    }

   /* public String getMulticastAddress(){
        return multicastAddress;
    }

    public MulticastSocket getSocket(){
        return socket;
    }*/

    public synchronized boolean isRunning(){
        return isRunning;
    }

    public synchronized void setIsRunning(boolean value){
        isRunning = value;
    }


    @Override
    public void run() {
        try {
            InetAddress group = InetAddress.getByName(multicastAddress);
            socket.joinGroup(group);
            while(isRunning()){
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                new Thread(new DiscoveryPacketHandler(packet)).start();
            }
            socket.leaveGroup(group);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
