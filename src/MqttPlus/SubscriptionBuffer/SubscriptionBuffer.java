package MqttPlus.SubscriptionBuffer;

import MqttPlus.Operators.Operator;
import MqttPlus.Utils.Matcher;
import MqttPlus.Operators.TemporalOperator;
import MqttPlus.Subscription.Subscription;
import MqttPlus.enums.InformationExtractionOperatorEnum;
import MqttPlus.enums.PeriodicOperatorEnum;
import MqttPlus.enums.TimeSpan;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class  SubscriptionBuffer {

    private static SubscriptionBuffer instance;
    private HashMap<String,ArrayList<Subscription>> normalSubcriptions;
    private HashMap<String,ArrayList<Subscription>> periodicSubscriptions;

    private SubscriptionBuffer() {
        this.normalSubcriptions = new HashMap<>();
        this.periodicSubscriptions = new HashMap<>();
    }

    public static synchronized SubscriptionBuffer getInstance(){
        if(instance==null){
            instance=new SubscriptionBuffer();
        }
        return instance;
    }

    public void insert(String subscriberId, Subscription sub){
        if(sub.isPeriodic()){
            insertPeriodic(subscriberId,sub);
        }
        else{
            insertNormal(subscriberId,sub);
        }
    }

    public boolean containsMatchingTemporalSubscription(String publishTopic){
        Set<Subscription> set = new HashSet<>();
        for(String key : normalSubcriptions.keySet()){
            set.addAll(normalSubcriptions.get(key));
        }

        for (Subscription subscription : set){
            if(Matcher.topicMatcher(publishTopic,subscription.getTopic())) return true;
        }
        return false;
    }


    public void insertNormal(String subscriberId,Subscription sub){
        if(!normalSubcriptions.containsKey(subscriberId)){
            ArrayList<Subscription> subscriptions = new ArrayList<>();
            normalSubcriptions.put(subscriberId,subscriptions);
        }
        ArrayList<Subscription> subscriptions = normalSubcriptions.get(subscriberId);

        if(!subscriptions.contains(sub)){
            subscriptions.add(sub);
        }

    }

    public void insertPeriodic(String subscriberId, Subscription sub){
        if(!periodicSubscriptions.containsKey(subscriberId)){
            ArrayList<Subscription> subscriptions = new ArrayList();
            periodicSubscriptions.put(subscriberId,subscriptions);
        }
        ArrayList<Subscription> subscriptions = periodicSubscriptions.get(subscriberId);
        subscriptions.add(sub);
    }

    @Override
    public synchronized String toString() {
        return "MqttPlus.MqttPlus.SubscriptionBuffer.SubscriptionBuffer{" +
                "normalSubcriptions=" + normalSubcriptions +
                '}';
    }

    public synchronized JSONObject toJsonObject(){

        JSONArray array = new JSONArray();

        for (String key : normalSubcriptions.keySet()){
            JSONObject object1 = new JSONObject();
            try {
                JSONArray array1 = new JSONArray();
                for (Subscription subscription : normalSubcriptions.get(key)){
                    array1.put(subscription.getCompleteTopic());
                }

                object1.put("Subscriptions",array1);
                object1.put("ClientID", key);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            array.put(object1);

        }

        JSONObject object = new JSONObject();
        try {
            object.put("SubscritpionBuffer" , array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }

    public ArrayList getClientSubscriptions(String subscriberId){
        return normalSubcriptions.get(subscriberId);
    }


    public ArrayList<Subscription> getNormalSubscriptions(){
        ArrayList<Subscription> arrayList = new ArrayList<>();

        for (Object key : normalSubcriptions.keySet()){
            arrayList.addAll(normalSubcriptions.get(key));
        }
        return arrayList;
    }

    public ArrayList<Subscription> getPeriodicSubscriptions(PeriodicOperatorEnum periodicOperator){
        ArrayList<Subscription> arrayList = new ArrayList<>();

        for (Object key : periodicSubscriptions.keySet()){
            for (Subscription subscription : periodicSubscriptions.get(key)){
                if(subscription.getPeriodicOperator().getPeriodicOperatorEnum()==periodicOperator){
                    arrayList.add(subscription);
                }
            }

        }

        return arrayList;
    }

    public boolean existMatchingSubscription(String pubTopic, InformationExtractionOperatorEnum operator){
        return existMatchingSubscription(pubTopic,operator,normalSubcriptions) || existMatchingSubscription(pubTopic,operator,periodicSubscriptions);

    }

    private boolean existMatchingSubscription(String pubTopic, InformationExtractionOperatorEnum operator, HashMap<String,ArrayList<Subscription>> buffer){
        for (String subcriberId : buffer.keySet()){
            for (Subscription subscription: buffer.get(subcriberId)){
                if(Matcher.topicMatcher(pubTopic,subscription.getTopic())){
                    if(subscription.containsOperator(operator)) return true;
                }
            }
        }
        return false;
    }

    public boolean existsMatchingSubSimple(String pubTopic){
        HashMap<String, ArrayList<Subscription>> buffer = new HashMap<>();
        buffer.putAll(normalSubcriptions);
        buffer.putAll(periodicSubscriptions);
        for (String subscriberId : buffer.keySet()){
            for (Subscription subscription:buffer.get(subscriberId)){
                if(Matcher.topicMatcher(pubTopic, subscription.getTopic()))
                    return true;
            }
        }
        return false;
    }


    public synchronized void removeClientSubscription(String subscriberId, String subTopic){
        ArrayList<Subscription> subscriptions = normalSubcriptions.get(subscriberId);
        if(subscriptions == null){
            subscriptions = periodicSubscriptions.get(subscriberId);
            if(subscriptions == null) return;
        }


        for (Subscription sub : subscriptions)
        {
            if(sub.getCompleteTopic().equals(subTopic)) {
                subscriptions.remove(sub);
                return;
            }
        }

    }

    public synchronized void removeAllClientSubcriptions(String subscriberId){
        normalSubcriptions.remove(subscriberId);
        periodicSubscriptions.remove(subscriberId);
    }

    public synchronized TimeSpan getMaxTimeSpan(String topic){
        TimeSpan maxTimeSpan = null;

        ArrayList<Subscription> subscriptions = new ArrayList<>(getAllSubscriptions());

        for (Subscription sub : subscriptions){
            if(Matcher.topicMatcher(topic,sub.getTopic())){
                TemporalOperator temporalOperator = sub.getTemporalOperator();
                if(temporalOperator != null){
                    TimeSpan timeSpan = temporalOperator.getTimeSpan();
                    if(maxTimeSpan== null){
                        maxTimeSpan = timeSpan;
                    }
                    else {
                        if(timeSpan.biggerThen(maxTimeSpan)){
                            maxTimeSpan = timeSpan;
                        }
                    }
                }
            }
        }
        return maxTimeSpan;
    }

    public synchronized Set<Subscription> getAllSubscriptions(){
        Set<Subscription> subscriptionSet = new HashSet<>();
        for(String key : normalSubcriptions.keySet()){
            subscriptionSet.addAll(normalSubcriptions.get(key));
        }
        return subscriptionSet;
    }

    public synchronized String subscriptionBufferToHTML(){
        String html = "<html>\n";
        html += "<head>\n" +
                "\t<meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\">\n" +
                "\t<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,user-scalable=no\">\n" +
                "\t<title>DataTables example - Multi-column ordering</title>\n" +
                "\t<link rel=\"shortcut icon\" type=\"image/png\" href=\"/media/images/favicon.png\">\n" +
                "\t<link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS 2.0\" href=\"http://www.datatables.net/rss.xml\">\n" +
                "\t<link rel=\"stylesheet\" type=\"text/css\" href=\"/media/css/site-examples.css?_=19472395a2969da78c8a4c707e72123a\">\n" +
                "\t<link rel=\"stylesheet\" type=\"text/css\" href=\"https://cdn.datatables.net/1.10.19/css/jquery.dataTables.min.css\">\n" +
                "\t<style type=\"text/css\" class=\"init\">\n" +
                "\t\n" +
                "\t</style>\n" +
                "\t<script type=\"text/javascript\" async=\"\" src=\"https://ssl.google-analytics.com/ga.js\"></script><script type=\"text/javascript\" src=\"/media/js/site.js?_=5e8f232afab336abc1a1b65046a73460\"></script>\n" +
                "\t<script type=\"text/javascript\" src=\"/media/js/dynamic.php?comments-page=examples%2Fbasic_init%2Fmulti_col_sort.html\" async=\"\"></script>\n" +
                "\t<script type=\"text/javascript\" language=\"javascript\" src=\"https://code.jquery.com/jquery-3.3.1.js\"></script>\n" +
                "\t<script type=\"text/javascript\" language=\"javascript\" src=\"https://cdn.datatables.net/1.10.19/js/jquery.dataTables.min.js\"></script>\n" +
                "\t<script type=\"text/javascript\" language=\"javascript\" src=\"../resources/demo.js\"></script>\n" +
                "\t<script type=\"text/javascript\" class=\"init\">\n" +
                "\t\n" +
                "\n" +
                "$(document).ready(function() {\n" +
                "\t$('#example').DataTable( {\n" +
                "\t\tcolumnDefs: [ {\n" +
                "\t\t\ttargets: [ 0 ],\n" +
                "\t\t\torderData: [ 0, 1 ]\n" +
                "\t\t}, {\n" +
                "\t\t\ttargets: [ 1 ],\n" +
                "\t\t\torderData: [ 1, 0 ]\n" +
                "\t\t}, {\"className\": \"dt-center\", \"targets\": \"_all\"} ]\n" +
                "\n" +
                "\t} );\n" +
                "} );\n" +
                "\n" +
                "\t</script>\n" +
                "</head>";
        html += "<body>\n";
        html += "<table id=\"example\">\n";
        html += "<thead>\n<tr>\n<th>SubscriberID</th>\n<th>Topic</th>\n</tr>\n</thead>\n";

        Set<String> keySet = new HashSet<>(normalSubcriptions.keySet());
        keySet.addAll(periodicSubscriptions.keySet());

        for(String key : keySet){
            ArrayList<Subscription> subscriptions = new ArrayList<>();
            if(normalSubcriptions.get(key) != null)
                subscriptions.addAll(normalSubcriptions.get(key));
            if(periodicSubscriptions.get(key)!=null)
                subscriptions.addAll(periodicSubscriptions.get(key));

            for(int i = 0; i< subscriptions.size() ; i++){
                html += "<tr>";
                html += "<td>"+ key + "</td>\n";
                html += "<td>" + subscriptions.get(i).getCompleteTopic() +"</td>";
                html += "</tr>";
            }
        }


        html += "</table>\n";
        html += "</body>\n";
        html += "</html>\n";
        return html;
    }

}
