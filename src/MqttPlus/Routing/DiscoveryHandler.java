package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;

import java.net.*;
import java.util.*;

public class DiscoveryHandler implements Runnable{
    private static DiscoveryHandler instance = null;
    private HashMap<String, String> discoveredAddresses;
    private DiscoveryReceiver discoveryReceiver;
    private DiscoverySender discoverySender;
    private Timer endTimer;
    private final long discoveryDuration = 10000; //duration of the discovery protocol in ms
    private long startingTime;
    private final String selfAddress = AdvertisementHandling.myHostname(JavaHTTPServer.local).split(":")[0] + ":" + JavaHTTPServer.PORT;
    private boolean isRunning;
    private final int RTTPort;
    private HashMap<String, String> RTTAddressMap;
    private DatagramSocket RTTSocket;
    private RTTHandler rttHandler;

    private DiscoveryHandler(){
        discoveredAddresses = new HashMap<>();
        discoverySender = new DiscoverySender();
        discoveryReceiver = DiscoveryReceiver.getInstance();
        RTTPort = chooseRTTPort();
        RTTAddressMap = new HashMap<>();
        endTimer = new Timer();
        isRunning = true;
    }

    public static DiscoveryHandler getInstance(){
        if(instance == null){
            instance = new DiscoveryHandler();
        }
        return instance;
    }


    public synchronized void insertDiscoveredAddress(String proxyAddress, String brokerAddress){
        discoveredAddresses.put(proxyAddress, brokerAddress);
    }

    public synchronized boolean isProxyDiscovered(String proxyAddress){
        return discoveredAddresses.containsKey(proxyAddress) || proxyAddress.equals(selfAddress);
    }

    public synchronized void insertDiscoveredRTTAddress(String proxy, String rttAddress){
        RTTAddressMap.put(proxy, rttAddress);
        System.out.println("RTTAddress Map: " + RTTAddressMap);
    }

    public synchronized Set<String> getProxies(){
        return discoveredAddresses.keySet();
    }

    public synchronized String getRTTAddress(String proxy){
        return RTTAddressMap.get(proxy);
    }

    @Override
    public void run(){
        discoverySender.start();
        discoveryReceiver.start();
        rttHandler = RTTHandler.getInstance();

        DiscoveryStopper stopper = new DiscoveryStopper(discoveryReceiver, discoverySender);
        endTimer.schedule(stopper, discoveryDuration);
        try {
            discoverySender.join();
        }catch (InterruptedException ex ){

        }
        while(getIsRunning()){
            while(!(JavaHTTPServer.getState().equals(ServerState.valueOf("DISCOVERY")))){
                synchronized (this) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            discoverySender = new DiscoverySender();
            discoverySender.start();
            endTimer = new Timer();
            stopper = new DiscoveryStopper(discoveryReceiver, discoverySender);
            endTimer.schedule(stopper, discoveryDuration);
            try {
                discoverySender.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public synchronized void clearDiscoveredAddresses(){
        System.out.println("ClearDiscoveredAddresses");
        discoveredAddresses.clear();
        discoverySender.finish();
    }
    public synchronized HashMap<String, String> getDiscoveredAddresses(){
        return (HashMap<String, String>) discoveredAddresses.clone();
    }

    public synchronized void stop(){
        this.isRunning = false;
        discoverySender.finish();
        discoveryReceiver.finish();
    }

    public synchronized boolean getIsRunning(){
        return isRunning;
    }

    private int chooseRTTPort(){
        boolean found = true;
        int port = 4447;

        do{
            found = !isPortInUse(port);
            if(!found)
                port++;
        }while(!found);

        return port;
    }

    private boolean isPortInUse(int port){
        try{
            InetAddress address = InetAddress.getByName(AdvertisementHandling.myHostname(JavaHTTPServer.local).split(":")[0]);
            DatagramSocket socket = new DatagramSocket(port);
            RTTSocket = socket;
            return false;
        } catch (SocketException e) {
            if(e instanceof BindException)
                return true;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return false;
    }

    public DatagramSocket getRTTSocket(){
        return RTTSocket;
    }
    public int getRTTPort(){
        return RTTPort;
    }


}
