package MqttPlus.Routing;

import java.util.ArrayList;
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


    public static boolean isAlreadyPublished(String topic){
        return publishedTopics.contains(topic);
    }

    public static void addTopic(String topic){
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
        Mqtt3Client client = ORT.getInstance().getClient(broker);
        synchronized (client) {
            if (!client.getState().isConnected()) {
                    client.toBlocking().connect();
            }
        }
        client.toBlocking().publishWith().topic(topic).payload(payload.toString().getBytes()).qos(MqttQos.AT_LEAST_ONCE).send();
    }

    public static String myHostname(boolean local){
        if(local){
            return new String("localhost:" + MQTTPublish.getBrokerPort());
        }else{
            String ip = null;
            try {
                URL whatismyip = new URL("http://checkip.amazonaws.com");
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        whatismyip.openStream()));
                ip = in.readLine(); //you get the IP as a String
            }finally {
                return new String(ip+":"+MQTTPublish.getBrokerPort());
            }
        }
    }


}
