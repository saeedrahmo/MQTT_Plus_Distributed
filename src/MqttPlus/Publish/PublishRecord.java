package MqttPlus.Publish;

import java.time.Instant;
import java.util.Date;

public class PublishRecord {

    Date publishDate;
    Double value;

    public PublishRecord(Double value){
        this.publishDate = Date.from(Instant.now());
        this.value = value;
    }

    public Date getPublishDate() {
        return publishDate;
    }

    public Double getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "MqttPlus.Publish.PublishRecord{" +
                "publishDate=" + publishDate +
                ", value=" + value +
                '}';
    }
}
