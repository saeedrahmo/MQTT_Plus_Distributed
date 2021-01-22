package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;

import java.net.*;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DiscoveryHandler implements Runnable{
    private static DiscoveryHandler instance = null;
    private HashMap<String, String> discoveredAddresses;
    private DiscoveryReceiver discoveryReceiver;
    private DiscoverySender discoverySender;
    private ScheduledExecutorService endTimer;
    private final long discoveryDuration = 10000; //duration of the discovery protocol in ms
    private long startingTime;
    private final String selfAddress = AdvertisementHandling.myHostname(JavaHTTPServer.local).split(":")[0] + ":" + JavaHTTPServer.PORT;
    private boolean isRunning;
    private final int RTTPort;
    private final int STPort;
    private HashMap<String, String> RTTAddressMap;
    private HashMap<String, String> STPAddressMap;
    private DatagramSocket RTTSocket;
    private DatagramSocket STPSocket;
    private RTTHandler rttHandler;
    private HashSet<String> discoveryMessageIDs;
    private HashSet<String> receivedMessageIDs;
    private boolean isRestarted;

    private DiscoveryHandler(){
        discoveredAddresses = new HashMap<>();
        discoverySender = new DiscoverySender();
        discoveryReceiver = DiscoveryReceiver.getInstance();
        receivedMessageIDs = new HashSet<>();
        discoveryMessageIDs = new HashSet<>();
        RTTPort = chooseRTTPort();
        STPort = chooseSTPort();
        RTTAddressMap = new HashMap<>();
        STPAddressMap = new HashMap<>();
        endTimer = Executors.newScheduledThreadPool(1);
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

    public synchronized void insertDiscoveryMessageID(String id){
        discoveryMessageIDs.add(id);
    }

    public synchronized void insertReceivedDiscoveryMessageID(String id){
        receivedMessageIDs.add(id);
    }

    public synchronized boolean isMessageIDReceived(String id){
        return receivedMessageIDs.contains(id);
    }

    public synchronized void clearReceivedMessageIDs(){
        receivedMessageIDs.clear();
    }

    public synchronized boolean isIDPresent(String id){
        return discoveryMessageIDs.contains(id);
    }

    public synchronized void clearDiscoveryMessageIDs(){
        discoveryMessageIDs.clear();
    }

    public synchronized boolean isProxyDiscovered(String proxyAddress){
        return discoveredAddresses.containsKey(proxyAddress) || proxyAddress.equals(selfAddress);
    }

    public synchronized void insertDiscoveredRTTAddress(String proxy, String rttAddress){
        RTTAddressMap.put(proxy, rttAddress);
        System.out.println("RTTAddress Map: " + RTTAddressMap);
    }

    public synchronized void insertDiscoveredSTPAddress(String proxy, String stpAddress){
        STPAddressMap.put(proxy, stpAddress);
        System.out.println("STPAddressMap Map: " + STPAddressMap);

    }

    public synchronized Set<String> getProxies(){
        return discoveredAddresses.keySet();
    }

    public synchronized String getRTTAddress(String proxy){
        return RTTAddressMap.get(proxy);
    }

    public String getSelfAddress() {
        return selfAddress;
    }

    public synchronized String getSTPAddress(String proxy){
        return STPAddressMap.get(proxy);
    }

    public synchronized String getBrokerAddress(String proxy){
        return discoveredAddresses.get(proxy);
    }

    @Override
    public void run(){
        discoveryReceiver.start();
        rttHandler = RTTHandler.getInstance();
        while(getIsRunning()){
            setIsRestarted(false);
            discoverySender = new DiscoverySender();
            discoverySender.start();
            endTimer = Executors.newScheduledThreadPool(1);
            DiscoveryStopper stopper = new DiscoveryStopper(discoveryReceiver, discoverySender);
            endTimer.schedule(stopper, discoveryDuration, TimeUnit.MILLISECONDS);
            synchronized (DiscoveryHandler.getInstance()) {
                while (JavaHTTPServer.getState().equals(ServerState.valueOf("DISCOVERY")) && !isRestarted()) {
                    try {
                        DiscoveryHandler.getInstance().wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            synchronized (DiscoveryHandler.getInstance()) {
                while (!(JavaHTTPServer.getState().equals(ServerState.valueOf("DISCOVERY"))) && !isRestarted()) {
                    try {
                        DiscoveryHandler.getInstance().wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }

        }

    }

    public synchronized void clearDiscoveredAddresses(){
        System.out.println("ClearDiscoveredAddresses");
        discoveredAddresses.clear();
        RTTAddressMap.clear();
        STPAddressMap.clear();
        discoverySender.finish();
        endTimer.shutdownNow();
        setIsRestarted(true);
        this.notifyAll();
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
            found = !isPortInUse(port, true);
            if(!found)
                port++;
        }while(!found);

        return port;
    }

    private int chooseSTPort(){
        boolean found = true;
        int port = 1024;

        do{
            found = !isPortInUse(port, false);
            if(!found)
                port++;
        }while(!found);

        return port;

    }

    private boolean isPortInUse(int port, boolean isRTT){
        try{
            InetAddress address = InetAddress.getByName(AdvertisementHandling.myHostname(JavaHTTPServer.local).split(":")[0]);
            DatagramSocket socket = new DatagramSocket(port);
            if(isRTT)
                RTTSocket = socket;
            else
                STPSocket = socket;
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

    public DatagramSocket getSTPSocket(){
        return STPSocket;
    }

    public int getSTPort(){
        return STPort;
    }
    public synchronized void setNewStopper(){
        synchronized (DiscoveryStopper.class) {
            if(!JavaHTTPServer.getState().equals(ServerState.valueOf("DISCOVERY"))) {
                endTimer.shutdownNow();
                endTimer = Executors.newScheduledThreadPool(1);
                DiscoveryStopper stopper = new DiscoveryStopper(discoveryReceiver, discoverySender);
                endTimer.schedule(stopper, discoveryDuration, TimeUnit.MILLISECONDS);
            }
        }
    }

    public synchronized boolean isRestarted(){
        return isRestarted;
    }

    public synchronized void setIsRestarted(boolean value){
        isRestarted = value;
    }


}
