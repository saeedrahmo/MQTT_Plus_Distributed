package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;
import MqttPlus.Utils.MQTTPublish;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.*;

public class ORT{

    private HashSet<String> oneHopBrokers;
    private HashMap<String, Mqtt3Client> clients;
    private ArrayList<String> clientIDs;

    //Here we don't use any HashMap since it's a representation of an overlay network


    private static ORT instance = null;

    private ORT(){
        oneHopBrokers = new HashSet<>();
        clients = new HashMap<>();
        clientIDs = new ArrayList<>();
    }

    public static ORT getInstance(){
        if(instance==null){
            instance = new ORT();
        }
        return instance;
    }


    public synchronized void insertHop(String broker){
        String hostname = broker.split(":")[0];
        String port = broker.split(":")[1];
        if (!clients.containsKey(broker)) {
            String id = ("ORT" + (new Integer(MQTTPublish.getBrokerPort()).toString())+ UUID.randomUUID().toString()).substring(0, 22);
            while (clientIDs.contains(id)){
                id = ("ORT" + (new Integer(MQTTPublish.getBrokerPort()).toString())+ UUID.randomUUID().toString()).substring(0, 22);
            }
            Mqtt3Client client = MqttClient.builder().identifier(id).serverPort(new Integer(port)).serverHost(hostname).useMqttVersion3().buildBlocking();
            clients.put(broker, client);

        }
            oneHopBrokers.add(broker);
    }
    public synchronized void removeHop(String broker){
        oneHopBrokers.remove(broker);
        clients.remove(broker);
    }

    public synchronized HashSet<String> getOneHopBrokers(){
        HashSet<String> result = new HashSet<>();
        result.addAll(oneHopBrokers);
        return result;
    }

    @Override
    public synchronized String toString(){
        return oneHopBrokers.toString();
    }

    public synchronized Mqtt3Client getClient(String broker){
        return clients.get(broker);
    }

    public synchronized void disconnectClients(){
        for(String broker : clients.keySet()){
            if(clients.get(broker).getState().isConnected()) {
                clients.get(broker).toBlocking().disconnect();
            }
        }
    }

    public synchronized boolean containsClientID(String id){
        return clientIDs.contains(id);
    }

}
