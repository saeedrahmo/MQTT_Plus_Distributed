package MqttPlus.Utils;

public class Matcher {

    public static boolean topicMatcher(String pubTopic, String subTopic){
        subTopic = subTopic.replaceAll("\\+",".*");
        subTopic = subTopic.replaceAll("#",".*\\$");
        subTopic = subTopic.replaceAll(";","|");
        subTopic = subTopic.replaceAll("/",")/(");
        subTopic = "(" + subTopic + ")";
        return pubTopic.matches(subTopic);
    }

}
