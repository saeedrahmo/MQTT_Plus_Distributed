package MqttPlus;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


import MqttPlus.Handlers.*;
import MqttPlus.PublishBuffers.CountFemaleBuffer;
import MqttPlus.PublishBuffers.CountMaleBuffer;
import MqttPlus.PublishBuffers.CountPeopleBuffer;
import MqttPlus.PublishBuffers.NumericBuffer;
import MqttPlus.Routing.*;
import MqttPlus.Subscription.Subscription;
import MqttPlus.SubscriptionBuffer.SubscriptionBuffer;
import MqttPlus.Utils.JSONUtility;
import MqttPlus.Utils.MQTTPublish;
import MqttPlus.Utils.Matcher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import MqttPlus.Publish.Publish;
import org.json.simple.parser.JSONParser;

public class JavaHTTPServer implements Runnable{


    private final String CONTENT_LENGTH = "content-length: ";
    private final String EMPTY_STRING = "";
    private final String MOSQUITTO = "mosquitto: ";
    private final String SUBSCRIPTION = "on_subscribe";
    private final String PUBLISH = "on_publish";
    private final String UNSUBSCRIPTION = "on_unsubscribe";
    private final String SYS_SUBSCRIPTION = "on_sys_subscribe";
    private final String DISCONNECT = "on_disconnect";
    private static ServerState state = ServerState.valueOf("DISCOVERY");
    private final DiscoveryHandler discoveryHandler;
    private final JSONUtility jsonUtility; //to be removed


    public static boolean local;
    public static boolean distributedProtocol = true;
    private static String topology;
    private static int numberOfBrokers;
    private static int count = 0;
    // port to listen connection
    public static int PORT;

    // verbose mode
    static final boolean verbose = true;

    // Client Connection via Socket Class
    private Socket connect;

    public JavaHTTPServer(Socket c, JSONUtility jsonUtility) {
        connect = c;
        discoveryHandler = DiscoveryHandler.getInstance();
        this.jsonUtility = jsonUtility;
    }


    public static void main(String[] args) {
        try {

            PORT = Integer.parseInt(args[0]);
            String brokerPort = args[1];
            distributedProtocol = new Boolean(args[2]);

            try{

                topology = new String(args[3]);
                numberOfBrokers = Integer.valueOf(args[4]);
                local = new Boolean(args[5]);

            }catch(IndexOutOfBoundsException ex){

            }
            MQTTPublish.setBrokerPort(brokerPort);
            if(distributedProtocol){

                //hardcodeORT();
                new Thread(DiscoveryHandler.getInstance()).start();
                //new Thread(JSONUtility.getInstance(topology, numberOfBrokers)).start();
            }
            ServerSocket serverConnect = new ServerSocket(PORT);
            System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");
            System.out.println(Instant.now().toString().split("T")[1].split("Z")[0].substring(0, 15));
            TimerHandler timerHandler = TimerHandler.getInstance();
            timerHandler.start();
            // we listen until user halts server execution
            Runtime.getRuntime().addShutdownHook(new Thread()
            {
                public void run()
                {
                        ORT.getInstance().disconnectClients();
                        SRT.getInstance().disconnectClients();
                        PRT.getInstance().disconnectClients();
                        DiscoveryHandler.getInstance().stop();
                        RTTHandler.getInstance().stop();
                        DiscoveryHandler.getInstance().stop();
                        MQTTPublish.disconnectClient();
                }
            });
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(200);
            while (true) {

                // create dedicated thread to manage the client connection
                Runnable myServer = new JavaHTTPServer(serverConnect.accept(), JSONUtility.getInstance(topology, numberOfBrokers));
                executor.submit(myServer);

                if (verbose) {
                    System.out.println("Connecton opened. (" + new Date() + ")");
                }

            }

        } catch (IOException e) {
            System.err.println("Server Connection error : " + e.getMessage());
        }
    }

    private static void hardcodeORT(){
        if(MQTTPublish.getBrokerPort()==1883){
            ORT.getInstance().insertHop("localhost:1884");
        }else if(MQTTPublish.getBrokerPort()==1884){
            ORT.getInstance().insertHop("localhost:1883");
            ORT.getInstance().insertHop("localhost:1885");
        }else if(MQTTPublish.getBrokerPort()==1885){
            ORT.getInstance().insertHop("localhost:1884");
        }else if(MQTTPublish.getBrokerPort() == 1886){
            ORT.getInstance().insertHop("localhost:1885");
        }else if(MQTTPublish.getBrokerPort() == 1887){
            ORT.getInstance().insertHop("localhost:1885");
        }else if(MQTTPublish.getBrokerPort() == 1888){
            ORT.getInstance().insertHop("localhost:1883");
        }
    }


    public void writeResponse(Object response){

        String contentType;
        if(response.getClass() == JSONObject.class){
            contentType = "application/json";
        }
        else {
            contentType = "text/html";
        }

        PrintWriter out=null;
        BufferedOutputStream dataOut=null;
        //System.out.println("Response \n"+ response.toString());
        try {
            out = new PrintWriter(connect.getOutputStream());
            dataOut = new BufferedOutputStream(connect.getOutputStream());

            out.println("HTTP/1.1 200 OK");
            out.println("Server: Java HTTP Server : 1.0");
            out.println("Date: " + new Date());
            out.println("Content-type: " + contentType);
            out.println("Content-length: " + response.toString().length());
            out.println();
            out.flush();

            byte[] responseBuffer=response.toString().getBytes();
            dataOut.write(responseBuffer,0,responseBuffer.length);
            dataOut.flush();


        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {

            try {
                connect.close();
                out.close();
                dataOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void run() {

        //continue to create thread to handle the clients request but block them until discovery is completed
        if(distributedProtocol) {
            synchronized (this.discoveryHandler) {
                while (!this.getState().equals(ServerState.valueOf("NORMAL"))) {
                    try {
                        this.discoveryHandler.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }

            System.out.println(JavaHTTPServer.getState());
        }

        BufferedReader in=null;
        String line;
        String action="";
        int length=0;
        count += 1 ;
        System.out.println("Thread numer " + count);
        try {
            in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            System.out.println("Headers: ");

            String input = in.readLine();
            StringTokenizer parse = new StringTokenizer(input);
            String method = parse.nextToken().toUpperCase();
            System.out.println(method);

            if(method.equals("GET")) {
                getResponse(parse.nextToken());
                return;
            }

            while (!((line = in.readLine()).equals(EMPTY_STRING))){
                System.out.println(line);
                if(line.toLowerCase().contains(CONTENT_LENGTH)) {
                    length = Integer.valueOf(line.substring(CONTENT_LENGTH.length()));
                }
                else if (line.contains(MOSQUITTO)){
                    action = line.substring(MOSQUITTO.length());
                }
            }

            char[] buf = new char[length];
            in.read(buf,0,length);
            String body = String.valueOf(buf);

            System.out.println(body);

            body = body.replace("\\","\\\\");
            JSONObject obj = new JSONObject(body);

            System.out.println(body);

            String timestamp = new String();

            if(action.equals(SUBSCRIPTION)){
                if(SubscriptionHandler.getInstance().isForwarded(obj) && distributedProtocol){
                    writeResponse(new JSONObject());
                    SubscriptionHandler.getInstance().forward(obj);
                    SubscriptionHandler.getInstance().insertRoutingInformation(obj);
                }else if(distributedProtocol){
                    JSONArray subscriptionArray = obj.getJSONArray("topics");
                    for (int i = 0;i < subscriptionArray.length();i++) {
                        JSONObject subObj = subscriptionArray.getJSONObject(i);
                        String topic = subObj.getString("topic");
                        SRT.getInstance().recordSubscriptionAdvEmpty(topic);
                    }
                    if(SubscriptionHandler.getInstance().isMqttPlus(obj)) {
                        writeResponse(SubscriptionHandler.getInstance().handleSubscription(obj));
                        SubscriptionBuffer buffer = SubscriptionBuffer.getInstance();
                    }else{
                        writeResponse(new JSONObject());
                        SubscriptionHandler.getInstance().forward(obj);
                    }
                }else if(SubscriptionHandler.getInstance().isMqttPlus(obj)){
                    writeResponse(SubscriptionHandler.getInstance().handleSubscription(obj));
                    SubscriptionBuffer buffer = SubscriptionBuffer.getInstance();
                }
            }
            else if(action.equals(PUBLISH)){
                writeResponse(new JSONObject());
                PublishHandler.getInstance().handlePublish(obj);
            }
            else if(action.equals(UNSUBSCRIPTION)){
                UnsubcriptionHandler.getInstance().handleUnsubscription(obj);
            }
            else if(action.equals(SYS_SUBSCRIPTION)){
                writeResponse(new JSONObject());
                SysSubscriptionHandler.getInstance().handleSysSubscription(obj);
            }
            else if(action.equals(DISCONNECT)){
                writeResponse(new JSONObject());
                UnsubcriptionHandler.getInstance().handleDisconnect(obj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void getResponse(String url){
        System.out.println(url);
        if(url.equals("/SubscriptionBuffer"))
            writeResponse(SubscriptionBuffer.getInstance().subscriptionBufferToHTML());
        else if(url.equals("/LVNumeric"))
            writeResponse(NumericBuffer.getInstance().lastValueBufferToHTML());
        else if(url.equals("/LVCountPeople"))
            writeResponse(CountPeopleBuffer.getInstance().lastValueBufferToHTML());
        else if(url.equals("/LVCountMale"))
            writeResponse(CountMaleBuffer.getInstance().lastValueBufferToHTML());
        else if(url.equals("/LVCountFemale"))
            writeResponse(CountFemaleBuffer.getInstance().lastValueBufferToHTML());
        else if(url.equals("/TmpNumeric"))
            writeResponse(NumericBuffer.getInstance().temporalBufferToHTML());
        else if(url.equals("/TmpCountPeople"))
            writeResponse(CountPeopleBuffer.getInstance().temporalBufferToHTML());
        else if(url.equals("/TmpCountMale"))
            writeResponse(CountMaleBuffer.getInstance().temporalBufferToHTML());
        else if(url.equals("/TmpCountFemale"))
            writeResponse(CountFemaleBuffer.getInstance().temporalBufferToHTML());

    }

    public static synchronized ServerState getState(){
        return state;
    }

    public static synchronized void setState(ServerState newState){
        state = newState;
    }


}