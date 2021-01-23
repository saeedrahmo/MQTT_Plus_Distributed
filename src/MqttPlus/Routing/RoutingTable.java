package MqttPlus.Routing;

import MqttPlus.JavaHTTPServer;
import MqttPlus.Utils.MQTTPublish;
import MqttPlus.Utils.Matcher;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import org.json.HTTP;

import java.util.*;

public abstract class RoutingTable {
    private final HashMap<String, HashSet<String>> routingTable;
    private HashMap<String, Mqtt3Client> clients;
    private ArrayList<String> clientIDs;

    //Hop format is "address:port"

    public RoutingTable(){
        routingTable = new HashMap<>();
        clients = new HashMap<>();
        clientIDs = new ArrayList<>();
    }


    public synchronized HashSet<String> brokerSetFromTopic(String topic){
        HashSet<String> result = new HashSet<>();
        for(String tableTopic:getTopics()){
            if(topicMatching(tableTopic, topic)){
                result.addAll(routingTable.get(tableTopic));
            }
        }
        return result;
    }

    public synchronized void insertTopic(String topic, String hop){
        String hostname = hop.split(":")[0];
        String port = hop.split(":")[1];
        if(!clients.containsKey(hop)){
            Mqtt3Client client;
            String id;
            if(this instanceof PRT){
                id = "PRT@" + DiscoveryHandler.getInstance().getSelfAddress() + UUID.randomUUID().toString();
                while(clientIDs.contains(id)){
                    id = "PRT@"+ DiscoveryHandler.getInstance().getSelfAddress() + UUID.randomUUID().toString();
                }
                client = MqttClient.builder().identifier(id).serverPort(new Integer(port)).serverHost(hostname).useMqttVersion3().buildBlocking();
                client.toAsync().connect();
                clients.put(hop, client);

            }

        }
        if(!routingTable.containsKey(topic)){
            routingTable.put(topic, new HashSet<>());
        }
        routingTable.get(topic).add(hop);
    }

    public synchronized void removeTopic(String topic){
        routingTable.remove(topic);
        for (String hop: clients.keySet()){
            if(routingTable.containsValue(hop)){
                clients.remove(hop);
            }
        }
    }

    public synchronized Set<String> getTopics(){
        return routingTable.keySet();
    }

    public synchronized boolean findTopic(String topic) {
        boolean result = false;
        for(String tableTopic: this.getTopics()){
            if(topicMatching(tableTopic, topic))
                result = true;
        }
        return result;
    }
    protected abstract boolean topicMatching(String tableTopic, String topic);


    public synchronized void printTable(){
        System.out.println(routingTable);
    }

    public synchronized Mqtt3Client getClient(String hop){
        return clients.get(hop);
    }

    public synchronized void disconnectClients(){
        for(String hop : clients.keySet()){
            if(clients.get(hop).getState().isConnected()) {
                clients.get(hop).toBlocking().disconnect();
            }
        }
    }
    public synchronized boolean containsClientID(String id){
        return clientIDs.contains(id);
    }

    public synchronized void clearTable(){
        routingTable.clear();
        clientIDs.clear();
        for (Mqtt3Client client: clients.values()){
            if(client.getState().isConnected()){
                client.toAsync().disconnect();
            }
        }
        clients.clear();
    }


}
