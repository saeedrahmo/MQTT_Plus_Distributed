package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;

import java.util.*;

public class DiscoveryHandler implements Runnable{
    private static DiscoveryHandler instance = null;
    private final String multicastAddress = "230.0.0.1";
    private HashMap<String, String> discoveredAddresses;
    private DiscoveryReceiver discoveryReceiver;
    private DiscoverySender discoverySender;
    private final Timer endTimer;
    private final long discoveryDuration = 10000; //duration of the discovery protocol in ms
    private long startingTime;
    private final String selfAddress = AdvertisementHandling.myHostname(JavaHTTPServer.local).split(":")[0] + ":" + JavaHTTPServer.PORT;

    private DiscoveryHandler(){
        discoveredAddresses = new HashMap<>();
        discoverySender = new DiscoverySender(multicastAddress);
        discoveryReceiver = new DiscoveryReceiver(multicastAddress);
        endTimer = new Timer();
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

    @Override
    public void run(){
        try {
            discoverySender.start();
            discoveryReceiver.start();
        }catch(IllegalThreadStateException ex){
            discoverySender = new DiscoverySender(multicastAddress);
            discoveryReceiver = new DiscoveryReceiver(multicastAddress);
            discoverySender.start();
            discoveryReceiver.start();
        }
        DiscoveryStopper stopper = new DiscoveryStopper(discoveryReceiver, discoverySender);
        endTimer.schedule(stopper, discoveryDuration);
        try {
            discoverySender.join();
            discoveryReceiver.join();
        }catch (InterruptedException ex ){

        }

    }

    public synchronized void setStartingTime(long time){
        startingTime = time;
    }

    public synchronized long getStartingTime(){
        return getStartingTime();
    }



}
