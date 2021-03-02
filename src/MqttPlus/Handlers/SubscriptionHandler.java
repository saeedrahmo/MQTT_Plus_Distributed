package MqttPlus.Handlers;

import MqttPlus.JavaHTTPServer;
import MqttPlus.Routing.AdvertisementHandling;
import MqttPlus.Routing.DiscoveryHandler;
import MqttPlus.Routing.PRT;
import MqttPlus.Routing.SRT;
import MqttPlus.Sematic.SemanticException;
import MqttPlus.Subscription.ParsingException;
import MqttPlus.Subscription.Subscription;
import MqttPlus.SubscriptionBuffer.SubscriptionBuffer;
import MqttPlus.Syntax.SyntaxException;
import MqttPlus.Utils.MQTTPublish;
import MqttPlus.Utils.Matcher;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.util.HashMap;
import java.util.UUID;

public class SubscriptionHandler {

    private final String TOPIC_KEY = "topics";
    private final String BRIDGE_KEY = "bridges";
    private final String SUBSCRIBER_KEY = "subscriberId";
    private final String RESULT_CODE = "resultCode";
    private final String TOPIC ="topic";
    private final String RESULT_MESSAGE = "resultMessage";
    private final String SUBCRIPTION_OK = "Subcription ok";
    private final String SUBCRIPTION_SYNTAX_ERROR = "Syntax error";
    private final String SUBCRIPTION_SEMANTIC_ERROR = "Semantic error";
    private final String SUBSCRIPTION_PARSING_ERROR = "Parsing error";
    private final int CODE_OK = 1;
    private final int SYNTAX_ERROR = 0;
    private final int SEMANTIC_ERROR = 0;
    private final int PARSING_ERROR = 0;

    private static SubscriptionHandler instance;

    private SubscriptionHandler(){
    }

    public static SubscriptionHandler getInstance(){
        if(instance==null){
            instance = new SubscriptionHandler();
        }
        return instance;
    }

    public boolean isForwarded(JSONObject obj){
        boolean result = false;
        try{
            JSONArray subscriptionArray = obj.getJSONArray(TOPIC_KEY);
            for (int i = 0;i < subscriptionArray.length();i++) {
                JSONObject subObj = subscriptionArray.getJSONObject(i);
                String topic = subObj.getString("topic");
                if(topic.contains("@")){
                    result = true;
                    break;
                }
            }
        }catch(JSONException ex){

        }
        return result;
    }

    public void forward(JSONObject obj){
        try{
            JSONArray subscriptionArray = obj.getJSONArray(TOPIC_KEY);
            for (int i = 0;i < subscriptionArray.length();i++) {
                JSONObject subObj = subscriptionArray.getJSONObject(i);
                String originHost = new String();
                String originalTopic = subObj.getString("topic");
                String topic = new String();
                if (originalTopic.contains("@")) {
                    originHost = originalTopic.split("@")[1];
                    topic = originalTopic.split("@")[0];

                }else{
                    topic = originalTopic;
                }
                System.out.println("Print SRT: ");
                SRT.getInstance().printTable();
                if (SRT.getInstance().findTopic(topic)) {
                    for (String broker : SRT.getInstance().brokerSetFromTopic(topic)) {
                        if(broker.compareTo(originHost)!=0 && SRT.getInstance().getSubscriptionAdvSent().keySet().contains(topic) && !SRT.getInstance().getSubscriptionAdvSent().get(topic).contains(broker)) {
                            sendSubscription(topic, broker);
                            SRT.getInstance().recordSubscriptionAdvSent(topic, broker);
                        }else if(broker.compareTo(originHost)!=0 && !SRT.getInstance().getSubscriptionAdvSent().keySet().contains(topic) && originalTopic.contains("@")){
                            sendSubscription(topic, broker);
                        }
                    }
                }

            }
        }catch(JSONException ex){

        }
    }

    public void insertRoutingInformation(JSONObject obj){
        try {
            JSONArray subscriptionArray = obj.getJSONArray(TOPIC_KEY);

            JSONArray returnArray = new JSONArray();
            String subscriberId = obj.getString(SUBSCRIBER_KEY);
            for (int i = 0; i < subscriptionArray.length(); i++) {
                JSONObject subObj = subscriptionArray.getJSONObject(i);
                String topic = subObj.getString("topic");
                if(topic.contains("@")){
                    PRT.getInstance().insertTopic(topic.split("@")[0], topic.split("@")[1]);
                }

            }
            System.out.println("Print PRT:");
            PRT.getInstance().printTable();

        }catch(JSONException ex){

        }
    }

    public void sendSubscription(String topic, String brokerAddress){
        /*String hostname = brokerAddress.split(":")[0];
        String port = brokerAddress.split(":")[1];
        Mqtt3Client client = MqttClient.builder().identifier(UUID.randomUUID().toString()).serverHost(hostname).serverPort(new Integer(port)).useMqttVersion3().build();
        client.toBlocking().connectWith().keepAlive(240);
        client.toBlocking().connect();*/

        String id = "SRT" + DiscoveryHandler.getInstance().getSelfAddress().split(":")[0]+":"+ MQTTPublish.getBrokerPort() + UUID.randomUUID().toString();
        while(SRT.getInstance().containsClientID(id)){
            id = "SRT" + DiscoveryHandler.getInstance().getSelfAddress().split(":")[0]+":"+ MQTTPublish.getBrokerPort() + UUID.randomUUID().toString();
        }
        String hostname = brokerAddress.split(":")[0];
        String port = brokerAddress.split(":")[1];
        Mqtt3Client client = MqttClient.builder().identifier(id).serverPort(new Integer(port)).serverHost(hostname).useMqttVersion3().buildBlocking();
        client.toAsync().connect();
        client.toAsync().subscribeWith().topicFilter(topic + "@" + AdvertisementHandling.myHostname(JavaHTTPServer.local)).qos(MqttQos.AT_LEAST_ONCE).send();
        client.toAsync().unsubscribeWith().topicFilter(topic + "@" + AdvertisementHandling.myHostname(JavaHTTPServer.local)).send();
        client.toAsync().disconnect();

    }


    public JSONObject handleSubscription(JSONObject obj){
        JSONObject returnObj = new JSONObject();
        SubscriptionBuffer buffer = SubscriptionBuffer.getInstance();
        if(JavaHTTPServer.distributedProtocol) {
            forward(obj);
        }

        try {
            JSONArray subscriptionArray = obj.getJSONArray(TOPIC_KEY);
            JSONArray returnArray = new JSONArray();
            String subscriberId = obj.getString(SUBSCRIBER_KEY);
            /*JSONArray bridgeArray = obj.getJSONArray(BRIDGE_KEY);
            for (int i = 0; i<bridgeArray.length(); i++){
                JSONObject bridge = bridgeArray.getJSONObject(i);
                String bridgeHostname = bridge.getString("address");
                int bridgePort = bridge.getInt("port");
                System.out.println("Brigde print: " + bridgeHostname + " " + bridgePort);
            }*/



            System.out.println("Received SUBSCRIPTION from CLIENT: " + subscriberId + " to topics: " + subscriptionArray );

            for (int i = 0;i < subscriptionArray.length();i++){
                JSONObject subObj = subscriptionArray.getJSONObject(i);
                String topic = subObj.getString("topic");
                JSONObject resObj = new JSONObject();

                try {
                    Subscription sub = Subscription.parse(topic);
                    if(sub.isPeriodic()){
                        buffer.insertPeriodic(subscriberId,sub);
                    }else {
                        buffer.insertNormal(subscriberId,sub);
                    }
                    resObj.put(RESULT_CODE,CODE_OK);
                    resObj.put(TOPIC, topic);
                    resObj.put(RESULT_MESSAGE, SUBCRIPTION_OK);
                } catch (SyntaxException e) {
                    e.printStackTrace();
                    resObj.put(RESULT_CODE, SYNTAX_ERROR);
                    resObj.put(TOPIC, topic);
                    resObj.put(RESULT_MESSAGE, SUBCRIPTION_SYNTAX_ERROR);
                } catch (SemanticException e) {
                    e.printStackTrace();
                    resObj.put(RESULT_CODE, SEMANTIC_ERROR);
                    resObj.put(TOPIC, topic);
                    resObj.put(RESULT_MESSAGE, SUBCRIPTION_SEMANTIC_ERROR);
                } catch (ParsingException e) {
                    e.printStackTrace();
                    resObj.put(RESULT_CODE, PARSING_ERROR);
                    resObj.put(TOPIC, topic);
                    resObj.put(RESULT_MESSAGE, SUBSCRIPTION_PARSING_ERROR);
                }

                returnArray.put(resObj);

                System.out.println("Result code:" + resObj.get(RESULT_CODE) + " Topic " + resObj.get(TOPIC) + " Result Message: " + resObj.get(RESULT_MESSAGE));

            }
            returnObj.put("topics",returnArray);


        } catch (JSONException e) {
            e.printStackTrace();
        }
        return returnObj;
    }

    public boolean isMqttPlus(JSONObject obj){
        boolean result = false;
        try{
            JSONArray subscriptionArray = obj.getJSONArray(TOPIC_KEY);
            for (int i = 0;i < subscriptionArray.length();i++) {
                JSONObject subObj = subscriptionArray.getJSONObject(i);
                String topic = subObj.getString("topic");
                if (topic.contains("$")){
                    result = true;
                    break;
                }

            }
        }catch(JSONException ex){

        }
        return result;
    }

}
