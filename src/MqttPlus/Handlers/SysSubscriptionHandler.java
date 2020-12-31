package MqttPlus.Handlers;

import MqttPlus.BrokerStatus;
import MqttPlus.Utils.MQTTPublish;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SysSubscriptionHandler {

    private final String CAPABILITIES = "capabilities";
    private final String RESULT_CODE = "resultCode";
    private final String TOPIC ="topic";
    private final String TOPICS = "topics";
    private final String RESULT_MESSAGE = "resultMessage";
    private final String SUBCRIPTION_OK = "Subscription ok";
    private final String SUBCRIPTION_ERROR = "Subscription error";
    private final int CODE_OK = 1;
    private final int CODE_ERROR = 0;

    private static SysSubscriptionHandler instance;

    private SysSubscriptionHandler(){

    }

    public static SysSubscriptionHandler getInstance(){
        if(instance==null){
            instance = new SysSubscriptionHandler();
        }
        return instance;
    }

    public void handleSysSubscription(JSONObject reqObj){

        try{
            JSONArray array = reqObj.getJSONArray(TOPICS);
            for (int i = 0; i < array.length();i++){
                JSONObject obj = array.getJSONObject(i);
                String topic = obj.getString(TOPIC);
                int split = topic.indexOf("/");
                String opPart = topic.substring(0,split);
                String topicPart = topic.substring(split + 1);

                if(topicPart.equals(CAPABILITIES)){
                    JSONArray object = new JSONArray(BrokerStatus.brokerFunctions());
                    MQTTPublish.sendPublish(topic,object);
                }

            }


        } catch (JSONException e) {
            e.printStackTrace();
        }


    }


}
