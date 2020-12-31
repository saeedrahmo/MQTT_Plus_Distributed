package MqttPlus.PublishBuffers;

import java.util.HashMap;

public class CountPeopleBuffer extends PublishBuffer{

    private static CountPeopleBuffer instance;

    private CountPeopleBuffer(){
        lastValueBuffer = new HashMap<>();
        temporalBuffer = new HashMap<>();
    }

    public static synchronized CountPeopleBuffer getInstance() {
        if(instance == null){
            instance = new CountPeopleBuffer();
        }
        return instance;
    }



}
