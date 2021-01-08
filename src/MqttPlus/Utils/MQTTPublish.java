package MqttPlus.Utils;


import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;


public class MQTTPublish {

    private static String broker = "tcp://localhost:";
    private static final String clientId  = "JavaWS";
    private static int brokerPort;
    private static Mqtt3Client client;

    public synchronized static void sendPublish(String topic,Object payload) {
        int qos = 2;
        if(!client.getState().isConnected()){
            client.toAsync().connect();
        }
        client.toAsync().publishWith().topic(topic).payload(payload.toString().getBytes()).qos(MqttQos.AT_LEAST_ONCE).send();
    }

    public static void setBrokerPort(String port){
        broker = broker.concat(port);
        brokerPort = Integer.parseInt(port);
        client = Mqtt3Client.builder().serverHost("localhost").serverPort(brokerPort).identifier(clientId).build();
    }

    public static int getBrokerPort(){
        return brokerPort;
    }

    public static void disconnectClient(){
        if(client.getState().isConnected()){
            client.toBlocking().disconnect();
        }
    }

}
