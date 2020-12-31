package MqttPlus.PublishBuffers;

import java.util.HashMap;

public class NumericBuffer extends PublishBuffer {
    private static NumericBuffer instance;

    private NumericBuffer(){
        super.lastValueBuffer = new HashMap<>();
        super.temporalBuffer = new HashMap<>();
    }

    public static synchronized NumericBuffer getInstance() {
        if(instance == null){
            instance = new NumericBuffer();
        }
        return instance;
    }
}
