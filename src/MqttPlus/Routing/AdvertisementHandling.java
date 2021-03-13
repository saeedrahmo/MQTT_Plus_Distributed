package MqttPlus.Routing;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;

import MqttPlus.Handlers.PublishHandler;
import MqttPlus.JavaHTTPServer;
import MqttPlus.Publish.Publish;
import MqttPlus.Utils.MQTTPublish;
import MqttPlus.enums.DataType;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import java.net.*;
import java.io.*;


public class AdvertisementHandling {

    private static final HashSet<String> publishedTopics = new HashSet<>();


    public static synchronized void clearTopics(){
        publishedTopics.clear();
    }

    public synchronized static boolean isAlreadyPublished(String topic){
        return publishedTopics.contains(topic);
    }

    public synchronized static void addTopic(String topic){
        publishedTopics.add(topic);
    }

    public static void advertise(String topic, Object payload, boolean local){
        ORT routingTable = ORT.getInstance();
        HashSet<String> oneHopBrokers = routingTable.getOneHopBrokers();
        String myHostname = myHostname(local);
        String finalTopic = topic + "@" + myHostname;
        for(String broker : oneHopBrokers){
            publish(broker, finalTopic, payload);
        }
    }

    public static void forward(String topic, Object payload, boolean local, String prevHost){
        ORT routingTable = ORT.getInstance();
        HashSet<String> oneHopBrokers = routingTable.getOneHopBrokers();
        String myHostname = myHostname(local);
        String finalTopic = topic + "@" + myHostname;
        for (String broker: oneHopBrokers){
            if(broker.compareTo(prevHost)!=0){
                publish(broker, finalTopic, payload);
            }
        }
    }

    private static void publish(String broker, String topic, Object payload){
        MqttClient client = ORT.getInstance().getClient(broker);
        synchronized (client) {
            if (!client.isConnected()) {
                try {
                    client.connect();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }
        MqttMessage message = new MqttMessage(payload.toString().getBytes());
        message.setQos(2);
        try {
            client.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public static String myHostname(boolean local) {
        if (local) {
            return new String("localhost:" + MQTTPublish.getBrokerPort());
        } else {
            String ip = null;
            Enumeration e = null;
            try {
                e = NetworkInterface.getNetworkInterfaces();
                while (e.hasMoreElements()) {
                    NetworkInterface n = (NetworkInterface) e.nextElement();
                    Enumeration ee = n.getInetAddresses();
                    while (ee.hasMoreElements()) {
                        InetAddress i = (InetAddress) ee.nextElement();
                        if (System.getenv("IP_ADDR") == null) {
                            if (i.getHostAddress().contains("172")) {
                                ip = i.getHostAddress();
                                return new String(ip + ":" + MQTTPublish.getBrokerPort());
                            }
                        } else {
                            ip = System.getenv("IP_ADDR");
                            return new String(ip + ":" + MQTTPublish.getBrokerPort());
                        }
                    }
                }
            } catch (SocketException socketException) {
                socketException.printStackTrace();
            }
            return new String("localhost:" + MQTTPublish.getBrokerPort());

        }
    }


}
