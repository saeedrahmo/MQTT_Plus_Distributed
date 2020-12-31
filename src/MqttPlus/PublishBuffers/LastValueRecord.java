package MqttPlus.PublishBuffers;

import MqttPlus.Publish.PublishRecord;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public class LastValueRecord {

    private final Duration expirationTime = Duration.ofHours(1);

    private Date expirationDate;
    private PublishRecord publishRecord;
    private Statistics statistics;

    public LastValueRecord(PublishRecord publishRecord){

        this.publishRecord = publishRecord;
        this.statistics = new Statistics();

    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public PublishRecord getPublishRecord() {
        return publishRecord;
    }

    public void updateStatistics(PublishRecord publishRecord){
        statistics.updateStatistics(publishRecord);
        this.publishRecord = publishRecord;
        this.expirationDate = Date.from(Instant.now().plus(expirationTime));
    }

    public Statistics getStatistics() {
        return statistics;
    }


    @Override
    public String toString() {
        return "MqttPlus.PublishBuffers.LastValueRecord{" +
                "expirationDate=" + expirationDate +
                ", publishRecord=" + publishRecord +
                ", statistics=" + statistics +
                "}\n";
    }
}
