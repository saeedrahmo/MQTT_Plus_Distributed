package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
        System.out.println("Inside RTTHandler");
        while(getIsRunning()){
            setRestart(false);
            for (String proxy : DiscoveryHandler.getInstance().getProxies()){
                System.out.println("PROXY: " + proxy);

                if(!(proxy.equals(AdvertisementHandling.myHostname(JavaHTTPServer.local).split(":")[0] + ":" + JavaHTTPServer.PORT))) {
                    String requestNumber = UUID.randomUUID().toString();
                    while(containsRequest(requestNumber)){
                        requestNumber = UUID.randomUUID().toString();
                    }
                    insertRequestNumber(requestNumber, proxy);
                    startingTimeTable.put(proxy, new Long(0));
                    retransmissionMap.put(requestNumber, new Integer(0));
                    RTTMsgSender sender = new RTTMsgSender(proxy, false, requestNumber);
                    sender.start();
                }
            }
            while((!rttComputedForHosts.containsAll(getStartingTimeTableKeySet()) && isRunning && !isRestarted()) || JavaHTTPServer.getState().equals(ServerState.valueOf("DISCOVERY"))){
                synchronized (this){
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("Server State: " + JavaHTTPServer.getState());

            if(!isRestarted()) {
                if (rttComputedForHosts.containsAll(getStartingTimeTableKeySet()) && !STPHandlerThread.isAlive()) {
                    JavaHTTPServer.setState(ServerState.valueOf("STP"));
                    STPHandlerThread.start();
                    synchronized (STPHandler.getInstance()) {
                        STPHandler.getInstance().notifyAll();
                    }
                } else if (restartSTP) {
                    //causa errore
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
        System.out.println("ADD STARTING TIME: "+startingTimeTable);
        System.out.println("RTT Table: " + RTTable);
    }

    public synchronized void computeRTT(String host){
        if(!isRestarted()) {
            RTTable.put(host, System.nanoTime() - startingTimeTable.get(host));
            rttComputedForHosts.add(host);
            startingTimeTable.put(host, new Long(0));
            System.out.println("condition: " + ((!rttComputedForHosts.containsAll(getStartingTimeTableKeySet()) && isRunning && !isRestarted()) || JavaHTTPServer.getState().equals(ServerState.valueOf("DISCOVERY"))));
            this.notifyAll();
            System.out.println("RTT:" + RTTable);
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
        System.out.println("restartHandler");
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

    public synchronized void removeExpirationTimer(String requestNumber){
        expirationMap.remove(requestNumber).shutdownNow();
    }

    public synchronized void insertExpirationTimer(String requestNumber){
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        expirationMap.put(requestNumber, service);
        service.schedule(new RTTRetransmissionTask(requestNumber), retransmissionDelay, TimeUnit.MILLISECONDS);
    }

    public synchronized Long getRTT(String host){
        return RTTable.get(host);
    }


}
