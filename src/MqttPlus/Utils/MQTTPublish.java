package MqttPlus.Utils;


import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MQTTPublish {

    private static String broker = "tcp://localhost:";
    private static final String clientId  = "JavaWS";
    private static int brokerPort;
    private static MqttClient client;

    public synchronized static void sendPublish(String topic,Object payload) {
        int qos = 2;
        if(!client.isConnected()){
            try {
                client.connect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        MqttMessage message = new MqttMessage(payload.toString().getBytes());
        message.setQos(qos);
        try {
            client.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public static void setBrokerPort(String port){
        broker = broker.concat(port);
        brokerPort = Integer.parseInt(port);
        try {
            client = new MqttClient(broker, clientId);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public static int getBrokerPort(){
        return brokerPort;
    }

    public static void disconnectClient(){
        if(client.isConnected()){
            try {
                client.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

}
