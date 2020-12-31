package MqttPlus;

import MqttPlus.Publish.Publish;
import MqttPlus.PublishBuffers.PublishBuffer;
import MqttPlus.Subscription.Subscription;
import MqttPlus.SubscriptionBuffer.SubscriptionBuffer;
import MqttPlus.Utils.Matcher;
import MqttPlus.enums.PeriodicOperatorEnum;

import java.util.*;

public class PublishVirtualizer {

    private static String SKR_STRING = "\\$A";

    private static boolean skr = true;

    public static HashMap rewrittenPublishes(Publish publish,PublishBuffer buffer){
        SubscriptionBuffer subscriptionBuffer = SubscriptionBuffer.getInstance();
        HashMap newPublishes = new HashMap();
        Set<Subscription> matchingTopics = new HashSet();

        ArrayList<Subscription> subcriptions = subscriptionBuffer.getNormalSubscriptions();


        for (Subscription subscription : subcriptions){
            Subscription sub = subscription;
            if(Matcher.topicMatcher(publish.getTopic(),sub.getTopic())){
                matchingTopics.add(sub);
            }
        }

        for (Subscription topic : matchingTopics){
            Subscription sub = topic;
            Publish newPublish = OperationProcessor.subcriptionProcess(publish,sub);
            if(newPublish.getPayload()!=null){
                if(skr){
                    newPublishes.put(singleKeyReplacement(newPublish.getTopic()),newPublish.getPayload());
                }
                else {
                    newPublishes.put(replacementWithParticipantTopics(newPublish.getTopic(),buffer),newPublish.getPayload());
                }
            }

        }
        return newPublishes;
    }

    public static HashMap<String,Object> rewrittenPeriodicPublish(PeriodicOperatorEnum periodicOperatorEnum){
        HashMap<String,Object> newPublishes = new HashMap<>();
        SubscriptionBuffer subscriptionBuffer = SubscriptionBuffer.getInstance();
        Set<Subscription> periodicSubscriptions = new HashSet<>(subscriptionBuffer.getPeriodicSubscriptions(periodicOperatorEnum));

        for (Subscription subscription : periodicSubscriptions){
            System.out.println(subscription);
            Publish newPublish = OperationProcessor.periodicSubsriptionProcess(subscription);
            if(newPublish.getPayload()!=null){
                if(skr){
                    newPublishes.put(singleKeyReplacement(newPublish.getTopic()),newPublish.getPayload());
                }
                else {
                    PublishBuffer buffer = PublishBuffer.chooseRightBuffer(subscription.getPeriodicOperator());
                    newPublishes.put(replacementWithParticipantTopics(newPublish.getTopic(),buffer),newPublish.getPayload());
                }

            }
        }

        return newPublishes;
    }


    public static String singleKeyReplacement(String subTopic){
        subTopic = subTopic.replaceAll("\\+", SKR_STRING);
        subTopic = subTopic.replaceAll("#",SKR_STRING);
        return subTopic;
    }

    public static String replacementWithParticipantTopics(String subTopic,PublishBuffer buffer){

        if(buffer == null) return singleKeyReplacement(subTopic);

        String operator = subTopic.substring(0,subTopic.indexOf("/"));
        String matchingPart = subTopic.substring(subTopic.indexOf("/")+1);

        Set<String> matchingTopics = new HashSet<>();
        for (String pubTopic : buffer.getAllTopics()){
            if(Matcher.topicMatcher(pubTopic,matchingPart)){
                matchingTopics.add(pubTopic);
            }
        }

        ArrayList<String> subTopicTokens = tokenizeTopic(matchingPart);

        int size = subTopicTokens.size();

        for(int i=0; i< size;i++){
            if(subTopicTokens.get(i).equals("+")){
                subTopicTokens.set(i,"");
                for (String matchingTopic : matchingTopics ){
                    ArrayList<String> matchingTopicToken = tokenizeTopic(matchingTopic);
                    if(!subTopicTokens.get(i).contains(matchingTopicToken.get(i))){
                        String string = subTopicTokens.get(i) + matchingTopicToken.get(i) + ";";
                        subTopicTokens.set(i, string);
                    }
                }
            }

        }

        if (subTopicTokens.get(size-1).equals("#")){
            subTopicTokens.set(size-1, "");
            for (String matchingTopic : matchingTopics){
                ArrayList<String> matchingTopicToken = tokenizeTopic(matchingTopic);
                for (int i = size -1; i < matchingTopicToken.size() ; i++){
                    if(i > subTopicTokens.size()-1){
                        subTopicTokens.add(matchingTopicToken.get(i) + ";");
                    }
                    else {
                        if(!subTopicTokens.get(i).contains(matchingTopicToken.get(i))){
                            String string = subTopicTokens.get(i) + matchingTopicToken.get(i) + ";";
                            subTopicTokens.set(i,string);
                        }
                    }
                }
            }
        }

        String returnSubTopic = operator + "/";

        for (String token : subTopicTokens){
            if(token.endsWith(";")){
                token = token.substring(0,token.length()-1);
            }
            returnSubTopic += token + "/";
        }
        returnSubTopic = returnSubTopic.substring(0,returnSubTopic.length()-1);

        return returnSubTopic;
    }

    private static ArrayList<String> tokenizeTopic(String topic){
        return new ArrayList<String>(Arrays.asList(topic.split("/")));
    }




}
