package MqttPlus.Routing;

import java.util.HashSet;
import java.util.Timer;

public class DiscoveryHandler implements Runnable{
    private static DiscoveryHandler instance = null;
    private final String multicastAddress = "230.0.0.1";
    private HashSet<String> discoveredAddresses;
    private DiscoveryReceiver discoveryReceiver;
    private DiscoverySender discoverySender;
    private final Timer endTimer;
    private final long discoveryDuration = 40000; //duration of the discovery protocol in ms
    private long startingTime;

    private DiscoveryHandler(){
        discoveredAddresses = new HashSet<>();
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

    public synchronized HashSet<String> getDiscoveredAddresses(){
        return discoveredAddresses;
    }

    public synchronized void insertDiscoveredAddress(String host){
        discoveredAddresses.add(host);
    }

    @Override
    public synchronized void run(){
        DiscoveryStopper stopper = new DiscoveryStopper(discoveryReceiver);
        try {
            discoverySender.start();
            discoveryReceiver.start();
        }catch(IllegalThreadStateException ex){
            discoverySender = new DiscoverySender(multicastAddress);
            discoveryReceiver = new DiscoveryReceiver(multicastAddress);
            discoverySender.start();
            discoveryReceiver.start();
        }
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
