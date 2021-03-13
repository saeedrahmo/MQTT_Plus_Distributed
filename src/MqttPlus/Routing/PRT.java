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
                MqttClient client = PRT.getInstance().getClient(broker);
                MqttMessage message = new MqttMessage(publish.getPayload().toString().getBytes());
                message.setQos(0);
                try {
                    client.publish(publish.getTopic(), message);
                } catch (MqttException e) {
                    e.printStackTrace();
                }

                //HiveMQ client: TODO remove
                        /*try {
                            Mqtt3Client client = MqttClient.builder().useMqttVersion3().identifier("HTTPServer@" + AdvertisementHandling.myHostname(JavaHTTPServer.local) + index.toString()).serverHost(hostname).serverPort(new Integer(port)).buildAsync();
                            client.toBlocking().connectWith().keepAlive(240).cleanSession(true);
                            client.toBlocking().connect();
                            client.toBlocking().publishWith().topic(publish.getTopic()).payload(publish.getPayload().toString().getBytes()).qos(MqttQos.AT_LEAST_ONCE).send();
                            client.toBlocking().disconnect();
                        }catch(Exception ex){
                           ex.printStackTrace();
                        }*/

                        //Paho client: TODO remove
                /*try {
                    MqttConnectOptions opts = new MqttConnectOptions();
                    MemoryPersistence memoryPersistence = new MemoryPersistence();
                    opts.setKeepAliveInterval(240);
                    MqttClient client = new MqttClient("tcp://" + broker, "HTTPServer@" + AdvertisementHandling.myHostname(JavaHTTPServer.local) + index.toString(), memoryPersistence);
                    client.connect(opts);
                    MqttMessage message = new MqttMessage(publish.getPayload().toString().getBytes());
                    message.setQos(2);
                    client.publish(publish.getTopic(), message);
                    client.disconnect();
                }catch (MqttPersistenceException e) {
                            e.printStackTrace();
                } catch (MqttSecurityException e) {
                            e.printStackTrace();
                } catch (MqttException e) {
                            e.printStackTrace();
                }*/
            }
        }
    }
}
