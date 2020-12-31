package MqttPlus;

import MqttPlus.PublishBuffers.CountFemaleBuffer;
import MqttPlus.PublishBuffers.CountMaleBuffer;
import MqttPlus.PublishBuffers.CountPeopleBuffer;
import MqttPlus.PublishBuffers.NumericBuffer;
import MqttPlus.Utils.MQTTPublish;
import MqttPlus.enums.PeriodicOperatorEnum;

import java.util.HashMap;
import java.util.TimerTask;

public class PeriodicOperation extends TimerTask {

    PeriodicOperatorEnum periodicOperator;

    public PeriodicOperation(PeriodicOperatorEnum periodicOperator){
        this.periodicOperator = periodicOperator;
    }

    @Override
    public void run() {
        HashMap<String,Object> newPublishes = PublishVirtualizer.rewrittenPeriodicPublish(periodicOperator);

        for (String key : newPublishes.keySet()){
            MQTTPublish.sendPublish(key,newPublishes.get(key));

        }

        NumericBuffer numericBuffer = NumericBuffer.getInstance();
        numericBuffer.resetBufferStatistics(periodicOperator);

        CountPeopleBuffer countPeopleBuffer = CountPeopleBuffer.getInstance();
        countPeopleBuffer.resetBufferStatistics(periodicOperator);

        CountMaleBuffer countMaleBuffer = CountMaleBuffer.getInstance();
        countMaleBuffer.resetBufferStatistics(periodicOperator);

        CountFemaleBuffer countFemaleBuffer = CountFemaleBuffer.getInstance();
        countFemaleBuffer.resetBufferStatistics(periodicOperator);

    }
}
