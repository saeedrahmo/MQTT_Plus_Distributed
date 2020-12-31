package MqttPlus.PublishBuffers;

import MqttPlus.Publish.PublishRecord;

public class Statistics {

    private StatisticsRecord quarterHourStatistics;
    private StatisticsRecord hourStatistics;
    private StatisticsRecord dailyStatistics;

    public Statistics(){
        quarterHourStatistics = new StatisticsRecord();
        hourStatistics = new StatisticsRecord();
        dailyStatistics = new StatisticsRecord();
    }


    public StatisticsRecord getQuarterHourStatistics() {
        return quarterHourStatistics;
    }

    public StatisticsRecord getHourStatistics() {
        return hourStatistics;
    }

    public StatisticsRecord getDailyStatistics() {
        return dailyStatistics;
    }

    public void updateStatistics(PublishRecord record){
        quarterHourStatistics.updateStatisticsRecord(record);
        hourStatistics.updateStatisticsRecord(record);
        dailyStatistics.updateStatisticsRecord(record);
    }

    public void resetQuarterHourlyStatistics(){
        quarterHourStatistics = new StatisticsRecord();
    }

    public void resetHourlyStatistics(){
        hourStatistics = new StatisticsRecord();
    }

    public void resetDailyStatistics(){
        dailyStatistics = new StatisticsRecord();
    }

    public void resetAllStatistics(){
        quarterHourStatistics.resetStatistics();
        hourStatistics.resetStatistics();
        dailyStatistics.resetStatistics();
    }

    @Override
    public String toString() {
        return "MqttPlus.PublishBuffers.Statistics{" +
                ", quarterHourStatistics=" + quarterHourStatistics +
                ", hourStatistics=" + hourStatistics +
                ", dailyStatistics=" + dailyStatistics +
                '}';
    }
}
