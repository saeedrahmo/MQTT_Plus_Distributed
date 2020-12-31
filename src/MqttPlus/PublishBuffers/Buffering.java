package MqttPlus.PublishBuffers;

import MqttPlus.Utils.Matcher;
import MqttPlus.Publish.PublishRecord;
import MqttPlus.Subscription.Subscription;
import MqttPlus.SubscriptionBuffer.SubscriptionBuffer;
import MqttPlus.enums.TimeSpan;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TimerTask;

public class Buffering extends TimerTask {


    @Override
    public void run() {
        PublishBuffer numericBuffer = NumericBuffer.getInstance();
        PublishBuffer countPeopleBuffer = CountPeopleBuffer.getInstance();
        PublishBuffer countMale = CountMaleBuffer.getInstance();
        PublishBuffer countFemale = CountFemaleBuffer.getInstance();


        delete(numericBuffer);
        delete(countPeopleBuffer);
        delete(countMale);
        delete(countFemale);


    }

    private void delete(PublishBuffer publishBuffer){
        deleteUnsubscription(publishBuffer);
        deleteExipredPublish(publishBuffer);
    }

    private synchronized void deleteExipredPublish(PublishBuffer publishBuffer){
        SubscriptionBuffer subscriptionBuffer = SubscriptionBuffer.getInstance();

        for (Iterator<String> topicIterator = publishBuffer.getTemporalBuffer().keySet().iterator();topicIterator.hasNext();){
            String pubTopic = topicIterator.next();
            TimeSpan maxTimeSpan = subscriptionBuffer.getMaxTimeSpan(pubTopic);
            ArrayList<PublishRecord> publishRecords = publishBuffer.getTemporalBuffer().get(pubTopic);
            for (Iterator<PublishRecord> iterator=publishRecords.iterator(); iterator.hasNext(); ){
                PublishRecord record = iterator.next();
                TimeSpan pubTimeSpan = new TimeSpan(record.getPublishDate());
                if(pubTimeSpan.biggerThen(maxTimeSpan)){
                    iterator.remove();
                }
                else break;

            }
            if(publishRecords.isEmpty()){
                topicIterator.remove();
            }

        }
    }

    private synchronized void deleteUnsubscription(PublishBuffer publishBuffer){
        Set<Subscription> subscriptionSet = SubscriptionBuffer.getInstance().getAllSubscriptions();

        for (Iterator<String> pubIterator = publishBuffer.getTemporalBuffer().keySet().iterator(); pubIterator.hasNext();) {
            String pubTopic = pubIterator.next();
            boolean remove = true;
            for (Iterator<Subscription> subIterator = subscriptionSet.iterator(); subIterator.hasNext(); ) {
                String subTopic = subIterator.next().getTopic();
                if (Matcher.topicMatcher(pubTopic,subTopic)){
                    remove = false;
                    break;
                }
            }
            if (remove) pubIterator.remove();
        }

    }

}
