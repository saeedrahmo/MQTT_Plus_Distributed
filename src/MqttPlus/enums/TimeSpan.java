package MqttPlus.enums;

import java.time.Instant;
import java.util.Date;

public class TimeSpan {

    private static final String COLON = ":";

    private int days;
    private int hours;
    private int minutes;

    public TimeSpan(int days, int hours, int minutes) {
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
    }

    public TimeSpan(String time){
        String[] split = time.split(COLON);
        days = Integer.valueOf(split[0]);
        hours = Integer.valueOf(split[1]);
        minutes = Integer.valueOf(split[2]);

    }

    public TimeSpan(Date startTime){
        this(startTime, Date.from(Instant.now()));
    }

    public TimeSpan(Date startTime, Date endTime){
        long millisDiff = endTime.getTime() - startTime.getTime();

        this.minutes = (int) (millisDiff / 60000 % 60);
        this.hours = (int) (millisDiff / 3600000 % 24);
        this.days = (int) (millisDiff / 86400000);

    }

    @Override
    public String toString() {
        return intToString(days) + COLON + intToString(hours) + COLON + intToString(minutes);
    }

    private String intToString(int integer){
        if(integer<10) return "0" + integer;
        else return String.valueOf(integer);
    }

    public int getDays() {
        return this.days;
    }

    public int getHours() {
        return this.hours;
    }

    public int getMinutes() {
        return this.minutes;
    }

    public boolean biggerThen(TimeSpan tw) {
        return tw == null?true:(this.days > tw.days?true:(this.hours > tw.hours?true:this.minutes > tw.minutes));
    }

}
