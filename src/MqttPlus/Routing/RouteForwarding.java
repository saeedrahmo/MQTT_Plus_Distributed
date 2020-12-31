package MqttPlus.Routing;

import MqttPlus.Utils.MQTTPublish;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class RouteForwarding {
    /*public synchronized static void loopForwardPublish(String topic, Object payload){
        MemoryPersistence persistence = new MemoryPersistence();
        //This static method is useful to republish a clean publish message (without the @ "field")

        try {
            MqttClient client = new MqttClient("tcp://localhost:"+MQTTPublish.getBrokerPort(), MqttClient.generateClientId(), persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            client.connect(connOpts);
            MqttMessage message = new MqttMessage(payload.toString().getBytes());
            message.setQos(2);
            client.publish(topic, message);
            client.disconnect();

        } catch (MqttException e) {

        }
    }*/


}
