package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;

import java.util.*;
import java.util.concurrent.*;

public class RTTHandler implements Runnable{

    private static RTTHandler instance = null;
    private static boolean isStarted = false;
    private HashMap<String, Long> startingTimeTable;
    private HashMap<String, ScheduledExecutorService> expirationMap;
    private HashMap<String, Integer> retransmissionMap;
    private RTTMsgReceiver receiver;
    private HashMap<String, Long> RTTable;
    private boolean isRunning;
    private boolean restart;
    private boolean restartSTP;
    private int port;
    private HashMap<String, String> requestNumbers;
    private STPHandler stpHandler;
    private Thread STPHandlerThread;
    private ArrayList<String> rttComputedForHosts;
    private final long retransmissionDelay = 10000;
    private int recomputeTopologyCounter;

    private RTTHandler(){
        stpHandler = STPHandler.getInstance();
        STPHandlerThread = new Thread(stpHandler);
        startingTimeTable = new HashMap<>();
        rttComputedForHosts= new ArrayList<>();
        expirationMap = new HashMap<>();
        retransmissionMap = new HashMap<>();
        receiver = RTTMsgReceiver.getInstance();
        receiver.start();
        RTTable = new HashMap<>();
        requestNumbers = new HashMap<>();
        isRunning = true;
        restart = false;
        restartSTP = false;
        recomputeTopologyCounter = 0;
    }

    public static RTTHandler getInstance(){
        if(instance == null){
            instance = new RTTHandler();
        }
        return instance;
    }

    @Override
    public void run() {
        setIsStarted(true);
        while(getIsRunning()){
            setRestart(false);
            RTTMsgSender.setRestarted(false);
            int proxyCount = 0;
            for (String proxy : DiscoveryHandler.getInstance().getProxies()){
                if(!(proxy.equals(AdvertisementHandling.myHostname(JavaHTTPServer.local).split(":")[0] + ":" + JavaHTTPServer.PORT))) {
                    String ip = DiscoveryHandler.getInstance().getSelfAddress().split(":")[0];
                    String requestNumber = UUID.randomUUID().toString().substring(0, 4) + ip.split("\\.")[0] + ip.split("\\.")[1] + ip.split("\\.")[2] + ip.split("\\.")[3];
                    while(containsRequest(requestNumber)){
                        requestNumber = UUID.randomUUID().toString().substring(0, 4) + ip.split("\\.")[0] + ip.split("\\.")[1] + ip.split("\\.")[2] + ip.split("\\.")[3];
                    }
                    insertRequestNumber(requestNumber, proxy);
                    startingTimeTable.put(proxy, new Long(0));
                    retransmissionMap.put(requestNumber, new Integer(0));
                    RTTMsgSender sender = new RTTMsgSender(proxy, false, requestNumber);
                    sender.start();
                    proxyCount++;
                }
            }
            synchronized (this) {
                while ((!areAllRTTComputed() && getIsRunning() && !isRestarted()) || JavaHTTPServer.getState().equals(ServerState.valueOf("DISCOVERY"))) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }

            System.out.println("Server State: " + JavaHTTPServer.getState());

            if(proxyCount != 0 && getRecomputeTopologyCounter() == 10 && !isRestarted() && areAllRTTComputed() && !STPHandler.getInstance().getOriginalRoot().equals(DiscoveryHandler.getInstance().getSelfAddress())){

                if((getRTT(STPHandler.getInstance().getOriginalRoot()) < STPHandler.getInstance().getPathCost()) && !JavaHTTPServer.getState().equals(ServerState.valueOf("STP"))) {
                    JavaHTTPServer.setState(ServerState.valueOf("STP"));
                    STPHandler.getInstance().restartProtocol();
                }else{
                    resetRecomputeTopologyCounter();
                }
                rttComputedForHosts.clear();
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            if(!isRestarted()) {
                if (areAllRTTComputed() && !STPHandlerThread.isAlive()) {
                    if(restartSTP){
                        restartSTP = false;
                    }
                    JavaHTTPServer.setState(ServerState.valueOf("STP"));
                    STPHandlerThread.start();
                    synchronized (STPHandler.getInstance()) {
                        STPHandler.getInstance().notifyAll();
                    }
                } else if (areAllRTTComputed() && restartSTP) {
                    JavaHTTPServer.setState(ServerState.valueOf("STP"));
                    STPHandler.getInstance().restartProtocol();
                    restartSTP = false;
                }
            }
            rttComputedForHosts.clear();
            try {

                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(!JavaHTTPServer.getState().equals(ServerState.valueOf("STP"))){
                incrementRecomputeTopologyCounter();
            }else{
                resetRecomputeTopologyCounter();
            }

        }

    }

    public synchronized static boolean isStarted(){
        return isStarted;
    }

    public static synchronized void setIsStarted(boolean value){
        isStarted = value;
    }

    public synchronized RTTMsgReceiver getReceiver(){
        return receiver;
    }

    public synchronized void addStartingTime(String host){
        startingTimeTable.put(host, System.nanoTime());
    }

    public synchronized void computeRTT(String host){
        if(!isRestarted()) {
            RTTable.put(host, System.nanoTime() - startingTimeTable.get(host));
            rttComputedForHosts.add(host);
            startingTimeTable.put(host, new Long(0));
            this.notifyAll();
        }
    }
    public synchronized void stop(){
        isRunning = false;
        receiver.finish();
    }
    public synchronized boolean getIsRunning(){
        return isRunning;
    }

    public synchronized void restartHandler(){
        retransmissionMap.clear();
        for(String requestNumber : expirationMap.keySet()){
            expirationMap.get(requestNumber).shutdownNow();
        }
        expirationMap.clear();
        requestNumbers.clear();
        RTTable.clear();
        rttComputedForHosts.clear();
        startingTimeTable.clear();
        restart = true;
        restartSTP = true;
        resetRecomputeTopologyCounter();
        RTTMsgSender.setRestarted(true);
        JavaHTTPServer.setState(ServerState.valueOf("RTT"));
        this.notifyAll();

    }

    public synchronized boolean isRestarted(){
        return restart;
    }
    public synchronized void setRestart(boolean value){
        restart = value;
    }

    public synchronized Set<String> getRTTableKeySet(){
        return RTTable.keySet();
    }

    public synchronized Set<String> getStartingTimeTableKeySet(){
        return startingTimeTable.keySet();
    }

    public synchronized void insertRequestNumber(String requestNumber, String proxy){
        requestNumbers.put(requestNumber, proxy);
    }

    public synchronized void removeRequestNumber(String requestNumber){
        requestNumbers.remove(requestNumber);
    }

    public synchronized boolean containsRequest(String requestNumber){
        return requestNumbers.keySet().contains(requestNumber);
    }

    public synchronized String getRequestDestination(String requestNumber){
        return requestNumbers.get(requestNumber);
    }

    public synchronized Integer getRetransmissionCount(String requestNumber){
        return retransmissionMap.get(requestNumber);
    }

    public synchronized void incrementRetransmissionCount(String requestNumber){
        int value = retransmissionMap.get(requestNumber);
        retransmissionMap.remove(requestNumber);
        retransmissionMap.put(requestNumber, value + 1);
    }

    public synchronized void removeRetransmissionCount(String requestNumber){
        retransmissionMap.remove(requestNumber);
    }

    public synchronized ScheduledExecutorService removeExpirationTimer(String requestNumber){
        ScheduledExecutorService service = expirationMap.remove(requestNumber);
        if(service!= null){
            service.shutdownNow();
        }
        return service;
    }

    public synchronized void insertExpirationTimer(String requestNumber){
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        expirationMap.put(requestNumber, service);
        service.schedule(new RTTRetransmissionTask(requestNumber), retransmissionDelay, TimeUnit.MILLISECONDS);
    }

    public synchronized void incrementRecomputeTopologyCounter(){
        recomputeTopologyCounter++;
    }

    public synchronized void resetRecomputeTopologyCounter(){
        recomputeTopologyCounter = 0;
    }

    public synchronized int getRecomputeTopologyCounter(){
        return recomputeTopologyCounter;
    }

    public synchronized Long getRTT(String host){
        return RTTable.get(host);
    }

    public synchronized boolean isRTTComputedForHostsEmpty(){
        return rttComputedForHosts.isEmpty();
    }

    public synchronized boolean areAllRTTComputed(){
        return rttComputedForHosts.containsAll(startingTimeTable.keySet());
    }

}
