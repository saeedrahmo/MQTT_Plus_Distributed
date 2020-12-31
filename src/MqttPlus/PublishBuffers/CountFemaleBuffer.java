package MqttPlus.PublishBuffers;

import java.util.HashMap;

public class CountFemaleBuffer extends PublishBuffer{

    private static CountFemaleBuffer instance;

    private CountFemaleBuffer(){
        lastValueBuffer = new HashMap<>();
        temporalBuffer = new HashMap<>();
    }

    public static synchronized CountFemaleBuffer getInstance() {
        if(instance == null){
            instance = new CountFemaleBuffer();
        }
        return instance;
    }
}
