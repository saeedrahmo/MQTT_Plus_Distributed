package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;
import MqttPlus.Utils.MQTTPublish;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttClient;

import java.util.*;

public class ORT{

    private HashSet<String> oneHopBrokers;
    private HashMap<String, MqttClient> clients;
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
            String id = "ORT" + DiscoveryHandler.getInstance().getSelfAddress().toString();
            while (clientIDs.contains(id)){
                id = "ORT" + DiscoveryHandler.getInstance().getSelfAddress();
            }
            MqttClient client = null;
            try {
                client = new MqttClient("tcp://"+broker, id);
            } catch (MqttException e) {
                e.printStackTrace();
            }
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

    public synchronized MqttClient getClient(String broker){
        return clients.get(broker);
    }

    public synchronized void disconnectClients(){
        for(String broker : clients.keySet()){
            if(clients.get(broker).isConnected()) {
                try {
                    clients.get(broker).disconnect();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized boolean containsClientID(String id){
        return clientIDs.contains(id);
    }

    public synchronized void clearTable(){
        oneHopBrokers.clear();
        clientIDs.clear();
        for (MqttClient client: clients.values()){
            if(client.isConnected()){
                try {
                    client.disconnect();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }
        clients.clear();
    }

}
