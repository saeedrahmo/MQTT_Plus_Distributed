package MqttPlus.Routing;

import MqttPlus.Utils.Matcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class SRT extends RoutingTable{

    private static SRT instance = null;
    private HashMap<String, HashSet<String>> subscriptionAdvSent;

    private SRT(){
        super();
        subscriptionAdvSent = new HashMap<>();
    }

    public synchronized static SRT getInstance(){
        if(instance==null){
            instance = new SRT();
        }
        return instance;
    }

    public synchronized void recordSubscriptionAdvSent(String subscription, String broker){
        if(subscriptionAdvSent.get(subscription)==null){
            subscriptionAdvSent.put(subscription, new HashSet<>());
        }
        subscriptionAdvSent.get(subscription).add(broker);
    }

    public synchronized void recordSubscriptionAdvEmpty(String subscription) {
        if(!subscriptionAdvSent.keySet().contains(subscription))
            subscriptionAdvSent.put(subscription, new HashSet<>());
    }

    public synchronized HashMap<String, HashSet<String>> getSubscriptionAdvSent(){
        return subscriptionAdvSent;
    }

    public synchronized HashSet<String> findSubAdv(String pubAdv, String broker){
        HashSet<String> result = new HashSet<>();
        for (String sub: subscriptionAdvSent.keySet()){
            if(topicMatching(pubAdv, sub) && !subscriptionAdvSent.get(sub).contains(broker)){
                result.add(sub);
            }
        }
        return result;
    }

    public synchronized boolean isSubAdvSent(String pubAdv, String broker){
        for (String sub: subscriptionAdvSent.keySet()){
            if(topicMatching(pubAdv, sub)){
                return subscriptionAdvSent.get(sub).contains(broker);
            }
        }
        return false;
    }


    @Override
    public boolean topicMatching(String tableTopic, String topic) {
        return Matcher.topicMatcher(tableTopic, topic.split("/", 2)[1]);
    }

    @Override
    public synchronized void clearTable() {
        super.clearTable();
        HashSet<String> subscriptionsToBeSaved = new HashSet<>();
        subscriptionsToBeSaved.addAll(subscriptionAdvSent.keySet());
        subscriptionAdvSent.clear();
        for (String sub : subscriptionsToBeSaved){
            recordSubscriptionAdvEmpty(sub);
        }
    }
}
