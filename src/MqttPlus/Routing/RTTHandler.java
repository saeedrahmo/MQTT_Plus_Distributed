package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class RTTHandler implements Runnable{

    private static RTTHandler instance = null;
    private static boolean isStarted = false;
    private HashMap<String, Long> startingTimeTable;
    private RTTMsgReceiver receiver;
    private HashMap<String, Long> RTTable;
    private boolean isRunning;
    private boolean restart;
    private boolean restartSTP;
    private int port;
    private HashSet<String> requestNumbers;
    private STPHandler stpHandler;
    private Thread STPHandlerThread;
    private ArrayList<String> rttComputedForHosts;

    private RTTHandler(){
        stpHandler = STPHandler.getInstance();
        STPHandlerThread = new Thread(stpHandler);
        startingTimeTable = new HashMap<>();
        rttComputedForHosts= new ArrayList<>();
        receiver = RTTMsgReceiver.getInstance();
        receiver.start();
        RTTable = new HashMap<>();
        requestNumbers = new HashSet<>();
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
                    insertRequestNumber(requestNumber);
                    startingTimeTable.put(proxy, new Long(0));
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

            if(rttComputedForHosts.containsAll(getStartingTimeTableKeySet()) && !STPHandlerThread.isAlive()){
                JavaHTTPServer.setState(ServerState.valueOf("STP"));
                synchronized (STPHandler.getInstance()){
                    STPHandler.getInstance().notifyAll();
                }
                STPHandlerThread.start();
            }
            if(restartSTP){
                STPHandler.getInstance().restartProtocol();
                JavaHTTPServer.setState(ServerState.valueOf("STP"));
                restartSTP = false;
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
            System.out.println(RTTable);
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
        RTTable.clear();
        rttComputedForHosts.clear();
        startingTimeTable.clear();
        restart = true;
        restartSTP = true;
        RTTMsgSender.setRestarted(true);
        this.notify();

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

    public synchronized void insertRequestNumber(String requestNumber){
        requestNumbers.add(requestNumber);
    }

    public synchronized void removeRequestNumber(String requestNumber){
        requestNumbers.remove(requestNumber);
    }

    public synchronized boolean containsRequest(String requestNumber){
        return requestNumbers.contains(requestNumber);
    }

    public synchronized Long getRTT(String host){
        return RTTable.get(host);
    }


}
