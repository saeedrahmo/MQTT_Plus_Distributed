package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;
import MqttPlus.Publish.Publish;
import MqttPlus.Utils.Matcher;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

public class PRT extends RoutingTable{

    private static PRT instance = null;
    private HashSet<Integer> usedClientIndex;
    private Random randomGenerator;
    private HashMap<String, MqttClient> clients;


    private PRT(){
        super();
        usedClientIndex = new HashSet<>();
        randomGenerator = new Random();
    }

    public static PRT getInstance(){
        if(instance==null){
            instance = new PRT();
        }
        return instance;
    }



    @Override
    public boolean topicMatching(String tableTopic, String topic) {
        //split is used to eliminate the $ operator during the comparison of the base topic
        return Matcher.topicMatcher(topic, tableTopic.split("/", 2)[1]);

    }

    public void forwardPublish(Publish publish){
        System.out.println("DENTRO FWD");
        for(String broker: brokerSetFromTopic(publish.getTopic())) {
            System.out.println("Dentro forward broker " +  broker);
            if (!publish.getClientId().contains(broker)) {
                Mqtt3Client client = PRT.getInstance().getClient(broker);
                client.toAsync().publishWith().topic(publish.getTopic()).payload(publish.getPayload().toString().getBytes()).qos(MqttQos.AT_MOST_ONCE).send();

            }
        }
    }
}
