package MqttPlus.Publish;

import org.json.JSONException;
import org.json.JSONObject;

public class Publish {

    private static final String CLIENT_ID = "clientId";
    private static final String TOPIC = "topic";
    private static final String PAYLOAD = "payload";

    private int publishId;
    private String clientId;
    private String topic;
    private Object payload;


    public Publish(String clientId,String topic,Object payload){
        this.clientId = clientId;
        this.topic = topic;
        this.payload = payload;
    }

    public Publish(Publish publish){
        this.clientId = publish.clientId;
        this.topic= publish.topic;
        this.payload = publish.payload;
    }

    public String getClientId() {
        return clientId;
    }

    public String getTopic() {
        return topic;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public static Publish parsePublish(JSONObject object){
        try {
            String clientId = object.getString(CLIENT_ID);
            String topic = object.getString(TOPIC);
            Object payload = object.get(PAYLOAD);
            return new Publish(clientId,topic,payload);
        } catch (JSONException e) {
            return null;
        }
    }


}
