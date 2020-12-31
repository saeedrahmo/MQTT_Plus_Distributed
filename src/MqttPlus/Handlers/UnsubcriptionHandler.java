package MqttPlus.Handlers;

import MqttPlus.SubscriptionBuffer.SubscriptionBuffer;
import org.json.JSONException;
import org.json.JSONObject;

public class UnsubcriptionHandler {

    private static UnsubcriptionHandler instance;
    private static final String SUBSCRIBERID = "subscriberId";
    private static final String TOPIC = "topic";

    private UnsubcriptionHandler(){
    }

    public static UnsubcriptionHandler getInstance(){
        if(instance==null){
            instance = new UnsubcriptionHandler();
        }
        return instance;
    }

    public void handleUnsubscription(JSONObject obj){

        String topic;
        String subscriberId;
        try {
            subscriberId = obj.getString(SUBSCRIBERID);
            topic = obj.getString(TOPIC);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        SubscriptionBuffer subscriptionBuffer = SubscriptionBuffer.getInstance();
        subscriptionBuffer.removeClientSubscription(subscriberId,topic);

    }

    public void handleDisconnect(JSONObject obj){
        String subscriberId;
        try {
            subscriberId = obj.getString(SUBSCRIBERID);
        }
        catch (JSONException e){
            e.printStackTrace();
            return;
        }
        SubscriptionBuffer subscriptionBuffer = SubscriptionBuffer.getInstance();
        subscriptionBuffer.removeAllClientSubcriptions(subscriberId);
    }
}
