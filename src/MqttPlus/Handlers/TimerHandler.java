package MqttPlus.Handlers;

import MqttPlus.PublishBuffers.Buffering;
import MqttPlus.PeriodicOperation;
import MqttPlus.enums.PeriodicOperatorEnum;
import java.util.Timer;
import java.util.TimerTask;

public class TimerHandler {

    private static final int QUAURTER_HOUR_SECONDS = 15*1000;
    private static final int HOUR_SECONDS = 30*1000;
    private static final int DAY_SECONDS = 60*1000;

    private static final int BUFFERING_SECONDS = 60*1000;

    private static TimerHandler instance;
    private Timer quarterHourTimer;
    private Timer hourTimer;
    private Timer dayTimer;
    private Timer bufferingTimer;

    private TimerHandler(){
        quarterHourTimer = new Timer();
        hourTimer = new Timer();
        dayTimer = new Timer();
        bufferingTimer = new Timer();
    }

    public static TimerHandler getInstance() {
        if (instance == null){
            instance = new TimerHandler();
        }
        return instance;
    }

    public void start(){
        TimerTask quarterHourTask = new PeriodicOperation(PeriodicOperatorEnum.QUARTERHOURLY);
        TimerTask hourTask = new PeriodicOperation(PeriodicOperatorEnum.HOURLY);
        TimerTask dayTask = new PeriodicOperation(PeriodicOperatorEnum.DAILY);
        TimerTask bufferingTask = new Buffering();

        quarterHourTimer.schedule(quarterHourTask,0,QUAURTER_HOUR_SECONDS);
        hourTimer.schedule(hourTask, 0,HOUR_SECONDS);
        dayTimer.schedule(dayTask,0,DAY_SECONDS);
        bufferingTimer.schedule(bufferingTask,0,BUFFERING_SECONDS);

    }



}
