package MqttPlus.PublishBuffers;

import MqttPlus.Publish.PublishRecord;

public class StatisticsRecord {

    private Double sum;
    private Double min;
    private Double max;
    private Double count;

    public StatisticsRecord(){
        this.sum = Double.NaN;
        this.min = Double.NaN;
        this.max = Double.NaN;
        this.count = Double.NaN;

    }

    public Double getSum() {
        return sum;
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    public Double getCount() {
        return count;
    }

    public Double getAvg(){
        if(count == 0) return Double.NaN;
        else return sum/count;
    }

    public void updateStatisticsRecord(PublishRecord record){

        Double value = record.getValue();

        max = max.isNaN() ? value : Double.max(max,value);
        min = min.isNaN() ? value : Double.min(min,value);
        sum = sum.isNaN() ? value : sum + value;
        count = count.isNaN() ? 1 : count + 1;
    }

    public void resetStatistics(){
        this.count = Double.NaN;
        this.max = Double.NaN;
        this.min = Double.NaN;
        this.sum = Double.NaN;
    }

    @Override
    public String toString() {
        return "MqttPlus.PublishBuffers.StatisticsRecord{" +
                "sum=" + sum +
                ", min=" + min +
                ", max=" + max +
                ", count=" + count +
                '}';
    }
}
