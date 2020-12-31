package MqttPlus.PublishBuffers;

import java.util.HashMap;

public class CountMaleBuffer extends PublishBuffer {

    private static CountMaleBuffer instance;

    private CountMaleBuffer(){
        lastValueBuffer = new HashMap<>();
        temporalBuffer = new HashMap<>();
    }

    public static synchronized CountMaleBuffer getInstance() {
        if(instance == null){
            instance = new CountMaleBuffer();
        }
        return instance;
    }
}
