package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;
import com.sun.management.OperatingSystemMXBean;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

public class STPHandler implements Runnable{

    private String root;
    private static STPHandler instance = null;
    private Double L;
    private Double M;
    private Double rootL;
    private Double rootM;
    private Long pathCost;
    private final STPReceiver receiver;
    private static final double alpha = 1.0;
    private static final double beta = 1.0;
    private HashSet<String> rootRequestSources;
    private HashSet<String> rootFinishedMessageSources;
    private HashSet<String> localRootMessageDestinations;
    private HashSet<String> rootFinishedMessageDestinations;
    private HashSet<String> children;
    private Timer endTimer;
    private boolean isRunning;
    private STPState stpState;
    private final long endDelay = 8000;
    private boolean waitForFinish;
    private String originalRoot;

    private STPHandler(){
        waitForFinish = false;
        pathCost = new Long(0);
        rootRequestSources = new HashSet<>();
        rootFinishedMessageSources = new HashSet<>();
        localRootMessageDestinations = new HashSet<>();
        rootFinishedMessageDestinations = new HashSet<>();
        endTimer = new Timer();
        children = new HashSet<>();
        stpState = STPState.valueOf("ROOT");
        isRunning = true;
        root = DiscoveryHandler.getInstance().getSelfAddress();
        receiver = STPReceiver.getInstance();
        if(!receiver.isAlive()) {
            receiver.start();
        }
        //The assumption of using a Linux machine is still valid, however Java would allow us to extend this possibility to other OSs.
        Runtime runtime = Runtime.getRuntime();
        Thread memoryThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Process readMemoryProcess = runtime.exec("cat /proc/meminfo");
                    InputStream memoryStream = readMemoryProcess.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(memoryStream));
                    while(true){
                        try {
                            if (!reader.ready()) break;
                            String line = reader.readLine();
                            if(line.contains("MemTotal")){
                                String[] partialResult = line.split(" ");
                                M = new Double(partialResult[7]);
                                rootM = M;
                                //The value of the memory saved is in kB
                                break;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                } catch (IOException e) {
                        e.printStackTrace();
                }

            }
        });
        Thread cpuThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Process readCPUProcess = runtime.exec("cat /proc/cpuinfo");
                    InputStream cpuStream = readCPUProcess.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(cpuStream));
                    while(true){
                        try {
                            if (!reader.ready()) break;
                            String line = reader.readLine();
                            if(line.contains("cpu MHz")){
                                String[] partialResult = line.split(" ");
                                L = new Double(partialResult[2]);
                                rootL = L;
                                //The L value is in MHz
                                break;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                } catch (IOException e) {
                        e.printStackTrace();
                }

            }
        });

        memoryThread.start();
        cpuThread.start();
        try {
            memoryThread.join();
            cpuThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static STPHandler getInstance(){
        if(instance == null){
            instance = new STPHandler();
        }

        return instance;
    }

    public synchronized boolean getIsRunning(){
        return isRunning;
    }

    public synchronized void setIsRunning(boolean value){
        isRunning = value;
    }

    public Double getL() {
        return L;
    }

    public Double getM() {
        return M;
    }

    public synchronized Double getRootM(){
        return rootM;
    }

    public synchronized Double getRootL(){
        return rootL;
    }

    public synchronized void setRootM(Double M){
        rootM = M;
    }

    public synchronized void setRootL(Double L){
        rootL = L;
    }

    public synchronized String getRoot(){
        return root;
    }

    public synchronized void setRoot(String value){
        root = value;
    }

    public synchronized void setPathCost(Long value){
        pathCost = value;
    }

    /*public synchronized Long pathCostComputation(){
        if(root.equals(DiscoveryHandler.getInstance().getSelfAddress())){
            return new Long(0);
        }else{
            return RTTHandler.getInstance().getRTT(root);
        }
    }*/

    public synchronized Long getPathCost(){
        return pathCost;
    }

    public synchronized STPState getState(){
        return stpState;
    }

    public synchronized void setState(STPState state){
        stpState = state;
    }

    public Double getAlpha(){
        return alpha;
    }

    public Double getBeta(){
        return beta;
    }

    public synchronized void insertRootRequestSource(String source){
        rootRequestSources.add(source);
    }

    public synchronized void clearRootRequestSources(){
        rootRequestSources.clear();
    }

    public synchronized void insertRootFinishedMessageSource(String source){
        rootFinishedMessageSources.add(source);
    }

    public synchronized void clearRootFinishedMessageSource(){
        rootFinishedMessageSources.clear();
    }

    public synchronized boolean canPassToNormal(){
        return rootFinishedMessageSources.containsAll(DiscoveryHandler.getInstance().getProxies()) && areRootFinishedMessagesSent();
    }

    public synchronized boolean isRootFinished(){
        return rootRequestSources.containsAll(DiscoveryHandler.getInstance().getProxies()) && areLocalRootMessagesSent();
    }

    public synchronized void insertLocalRootMessageDestination(String dest){
        localRootMessageDestinations.add(dest);
    }

    public synchronized void clearLocalRootMessageDestination(){
        localRootMessageDestinations.clear();
    }

    public synchronized boolean areLocalRootMessagesSent(){
        return localRootMessageDestinations.containsAll(DiscoveryHandler.getInstance().getProxies());
    }

    public synchronized void insertRootFinishedMessageDestination(String dest){
        rootFinishedMessageDestinations.add(dest);
    }

    public synchronized void clearRootFinishedMessageDestination(){
        rootFinishedMessageDestinations.clear();
    }

    public synchronized boolean areRootFinishedMessagesSent(){
        return rootFinishedMessageDestinations.containsAll(DiscoveryHandler.getInstance().getProxies());
    }

    public synchronized void insertChild(String child){
        children.add(child);
    }

    public synchronized void removeChild(String child){
        children.remove(child);
    }

    public synchronized boolean containsChild(String child){
        return children.contains(child);
    }

    public synchronized void setOriginalRoot(String originalRoot){
        this.originalRoot = originalRoot;
    }

    public synchronized String getOriginalRoot(){
        return originalRoot;
    }

    @Override
    public void run() {
        while(getIsRunning()){
            for(String proxy:DiscoveryHandler.getInstance().getProxies()){
                System.out.println("PROXY INSIDE STP HANDLER:" + proxy);
                new STPSender(proxy, false).start();
            }
            while(waitForFinish){
                synchronized (STPHandler.getInstance()){
                    try {
                        STPHandler.getInstance().wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            //TODO verify the condition below

            while((STPHandler.getInstance().getState().equals(STPState.valueOf("ROOT")) || STPHandler.getInstance().getState().equals(STPState.valueOf("FINISHED"))) || !JavaHTTPServer.getState().equals(ServerState.valueOf("STP"))){
                synchronized (STPHandler.getInstance()){
                    try {
                        STPHandler.getInstance().wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            System.out.println("Server state inside STP: " + JavaHTTPServer.getState());
            System.out.println("STP STATE: " + getState());

            if(getState().equals(STPState.valueOf("RESTARTED"))){
                setState(STPState.valueOf("ROOT"));
            }
            if(getState().equals(STPState.valueOf("NORMAL"))){
                waitForFinish = true;
            }
            if(!getState().equals(STPState.valueOf("ROOT"))) {
                scheduleEndTimer();
            }


        }
    }

    public synchronized void sendFinishRootPhase(){
        for (String proxy : DiscoveryHandler.getInstance().getProxies()){
            new STPSender(proxy, true).start();
        }
    }

    public synchronized void scheduleEndTimer(){
        endTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                for (String child:children){
                    ORT.getInstance().insertHop(DiscoveryHandler.getInstance().getBrokerAddress(child));
                }
                if(!root.equals(DiscoveryHandler.getInstance().getSelfAddress())){
                    ORT.getInstance().insertHop(DiscoveryHandler.getInstance().getBrokerAddress(root));
                }
                System.out.println("Dentro STP:" + ORT.getInstance().toString());
                STPHandler.getInstance().setState(STPState.valueOf("FINISHED"));
                waitForFinish = false;
                synchronized (DiscoveryHandler.getInstance()) {
                    JavaHTTPServer.setState(ServerState.valueOf("NORMAL"));
                    DiscoveryHandler.getInstance().notifyAll();
                }
                synchronized (STPHandler.getInstance()){
                    STPHandler.getInstance().notifyAll();
                }
            }
        }, endDelay);
    }

    public synchronized void cancelTimer(){
        endTimer.cancel();
        endTimer = new Timer();
    }

    public synchronized void restartProtocol(){
        RTTHandler.getInstance().resetRecomputeTopologyCounter();
        endTimer.cancel();
        endTimer = new Timer();
        root = DiscoveryHandler.getInstance().getSelfAddress();
        pathCost = new Long(0);
        rootL = L;
        rootM = M;
        rootFinishedMessageDestinations.clear();
        rootRequestSources.clear();
        rootFinishedMessageSources.clear();
        localRootMessageDestinations.clear();
        children.clear();
        ORT.getInstance().clearTable();
        SRT.getInstance().clearTable();
        PRT.getInstance().clearTable();
        AdvertisementHandling.clearTopics();
        setState(STPState.valueOf("RESTARTED"));
        STPHandler.getInstance().notifyAll();
        waitForFinish = false;
    }


}


