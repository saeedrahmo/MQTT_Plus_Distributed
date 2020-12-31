package MqttPlus.PublishBuffers;

import MqttPlus.Operators.InformationExtractionOperator;
import MqttPlus.Operators.Operator;
import MqttPlus.Publish.PublishRecord;
import MqttPlus.SubscriptionBuffer.SubscriptionBuffer;
import MqttPlus.Utils.Matcher;
import MqttPlus.enums.LastValueOperatorEnum;
import MqttPlus.enums.PeriodicOperatorEnum;
import MqttPlus.enums.TimeSpan;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.text.SimpleDateFormat;
import java.util.*;

public abstract class PublishBuffer {

    protected HashMap<String, LastValueRecord> lastValueBuffer;
    protected HashMap<String,ArrayList<PublishRecord>> temporalBuffer;

    public static PublishBuffer chooseRightBuffer(Operator bufferOp){
        PublishBuffer publishBuffer;


        if(bufferOp.getClass() == InformationExtractionOperator.class){
            switch (((InformationExtractionOperator)bufferOp).getInformationExtractionOperatorEnum()){
                case COUNTPEOPLE:
                    publishBuffer = CountPeopleBuffer.getInstance();
                    break;
                case COUNTMALE:
                    publishBuffer = CountMaleBuffer.getInstance();
                    break;
                case COUNTFEMALE:
                    publishBuffer = CountFemaleBuffer.getInstance();
                    break;
                default:
                    publishBuffer = null;
            }
        }
        else {
            publishBuffer = NumericBuffer.getInstance();
        }


        return publishBuffer;
    }

    public synchronized void insertLastValue(String topic,Double value){
        PublishRecord publishRecord = new PublishRecord(value);
        if(!lastValueBuffer.containsKey(topic)){
            LastValueRecord lastValueRecord = new LastValueRecord(publishRecord);
            lastValueBuffer.put(topic,lastValueRecord);
        }
        lastValueBuffer.get(topic).updateStatistics(publishRecord);
        System.out.println(lastValueBuffer.get(topic).toString());

    }

    public synchronized void insertTemporalValue(String topic,Double value){
        SubscriptionBuffer subscriptionBuffer = SubscriptionBuffer.getInstance();

        if(!subscriptionBuffer.containsMatchingTemporalSubscription(topic)) return;

        PublishRecord publishRecord = new PublishRecord(value);
        if(!temporalBuffer.containsKey(topic)){
            ArrayList<PublishRecord> arrayList = new ArrayList<>();
            temporalBuffer.put(topic,arrayList);
        }

        ArrayList<PublishRecord> arrayList = temporalBuffer.get(topic);
        arrayList.add(publishRecord);

    }

    public synchronized String lastValueBufferToString(){
        return lastValueBuffer.toString();
    }

    public synchronized String temporalValueBufferToString() {
        String text = "Publish Buffer : \n";
        for(String key : temporalBuffer.keySet()){
            text +="Count:" + temporalBuffer.get(key).size() + key + temporalBuffer.get(key).toString() + "\n";
        }
        return text;
    }

    public synchronized ArrayList<Double> getMatchingLastValue(String subTopic){
        ArrayList<Double> values = new ArrayList<>();
        for (String pubTopic : lastValueBuffer.keySet()){
            if(Matcher.topicMatcher(pubTopic,subTopic)){
                if((new Date()).before(lastValueBuffer.get(pubTopic).getExpirationDate())){
                    values.add(lastValueBuffer.get(pubTopic).getPublishRecord().getValue());
                }
            }
        }
        return values;
    }

    public synchronized StatisticsRecord getStatisticsRecord(String pubTopic, PeriodicOperatorEnum periodicOperator){

        if(periodicOperator== PeriodicOperatorEnum.QUARTERHOURLY){
            return lastValueBuffer.get(pubTopic).getStatistics().getQuarterHourStatistics();
        }
        else if(periodicOperator == PeriodicOperatorEnum.HOURLY){
            return lastValueBuffer.get(pubTopic).getStatistics().getHourStatistics();
        }
        else if(periodicOperator == PeriodicOperatorEnum.DAILY){
            return lastValueBuffer.get(pubTopic).getStatistics().getDailyStatistics();
        }
        return null;
    }

    public synchronized ArrayList<Double> getMatchingValue(String subTopic, LastValueOperatorEnum op, PeriodicOperatorEnum periodicOperator){
        ArrayList<Double> matches = new ArrayList();

        if(periodicOperator == null) return getMatchingLastValue(subTopic);

        for (String pubTopic : lastValueBuffer.keySet()){
            if(Matcher.topicMatcher(pubTopic,subTopic)){
                StatisticsRecord statisticsRecord = getStatisticsRecord(pubTopic,periodicOperator);
                Double value = Double.NaN;
                switch (op){
                    case AVG:
                        value = statisticsRecord.getAvg();
                        break;
                    case COUNT:
                        value = statisticsRecord.getCount();
                        break;
                    case SUM:
                        value = statisticsRecord.getSum();
                        break;
                    case MAX:
                        value = statisticsRecord.getMax();
                        break;
                    case MIN:
                        value = statisticsRecord.getMin();
                        break;
                }
                if(!value.isNaN())
                    matches.add(value);
            }
        }
        return matches;
    }



    public synchronized ArrayList<Double> getMatchingTemporalBuffer(String subTopic, TimeSpan timeSpan){
        ArrayList<Double> arrayList = new ArrayList<>();

        for (String key : temporalBuffer.keySet())
        {
            if(Matcher.topicMatcher(key,subTopic)){
                for (PublishRecord record : temporalBuffer.get(key)){
                    TimeSpan timeSpan1 = new TimeSpan(record.getPublishDate());
                    if(timeSpan.biggerThen(timeSpan1)){
                        arrayList.add(record.getValue());
                    }
                }
            }
        }

        return arrayList;
    }

    public synchronized void resetBufferStatistics(PeriodicOperatorEnum periodicOperatorEnum){
        for (String key : lastValueBuffer.keySet()){
            switch (periodicOperatorEnum){
                case QUARTERHOURLY:
                    lastValueBuffer.get(key).getStatistics().resetQuarterHourlyStatistics();
                    break;
                case HOURLY:
                    lastValueBuffer.get(key).getStatistics().resetHourlyStatistics();
                    break;
                case DAILY:
                    lastValueBuffer.get(key).getStatistics().resetDailyStatistics();
                    break;
            }
        }

    }

    public synchronized Double getLastValue(String topic){
        LastValueRecord lastValueRecord = lastValueBuffer.get(topic);
        if(lastValueRecord != null)
            return lastValueBuffer.get(topic).getPublishRecord().getValue();
        return null;
    }

    public synchronized HashMap<String, ArrayList<PublishRecord>> getTemporalBuffer() {
        return temporalBuffer;
    }

    public synchronized Set<String> getAllTopics(){
        Set<String> matchingTopics = new HashSet<>();
        matchingTopics.addAll(lastValueBuffer.keySet());
        matchingTopics.addAll(temporalBuffer.keySet());
        return matchingTopics;
    }

    public synchronized JSONObject lastValueBufferToJsonObject(){
        JSONArray array = new JSONArray();
        for (String key : lastValueBuffer.keySet()){
            LastValueRecord lastValueRecord = lastValueBuffer.get(key);
            JSONObject object = new JSONObject();
            try {
                object.put(key,lastValueRecord.toString());
                array.put(object);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        JSONObject object = new JSONObject();
        try {
            object.put("LastValueBuffer",array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }

    public synchronized JSONObject temporalBufferToJsonObject(){
        JSONArray array = new JSONArray();
        for (String key : temporalBuffer.keySet()){

            ArrayList<PublishRecord> publishRecordArrayList = temporalBuffer.get(key);
            JSONObject object = new JSONObject();
            JSONArray array1 = new JSONArray();
            for (PublishRecord publishRecord : publishRecordArrayList){
                array1.put(publishRecord.toString());
            }
            try {
                object.put(key,array1);
                array.put(object);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        JSONObject object = new JSONObject();
        try {
            object.put("TemporalBuffer",array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }

    public synchronized String temporalBufferToHTML(){
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String html = "<html>\n";
        html += "<head>\n" +
                "\t<meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\">\n" +
                "\t<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,user-scalable=no\">\n" +
                "\t<title>DataTables example - Multi-column ordering</title>\n" +
                "\t<link rel=\"shortcut icon\" type=\"image/png\" href=\"/media/images/favicon.png\">\n" +
                "\t<link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS 2.0\" href=\"http://www.datatables.net/rss.xml\">\n" +
                "\t<link rel=\"stylesheet\" type=\"text/css\" href=\"/media/css/site-examples.css?_=19472395a2969da78c8a4c707e72123a\">\n" +
                "\t<link rel=\"stylesheet\" type=\"text/css\" href=\"https://cdn.datatables.net/1.10.19/css/jquery.dataTables.min.css\">\n" +
                "\t<style type=\"text/css\" class=\"init\">\n" +
                "\t\n" +
                "\t</style>\n" +
                "\t<script type=\"text/javascript\" async=\"\" src=\"https://ssl.google-analytics.com/ga.js\"></script><script type=\"text/javascript\" src=\"/media/js/site.js?_=5e8f232afab336abc1a1b65046a73460\"></script>\n" +
                "\t<script type=\"text/javascript\" src=\"/media/js/dynamic.php?comments-page=examples%2Fbasic_init%2Fmulti_col_sort.html\" async=\"\"></script>\n" +
                "\t<script type=\"text/javascript\" language=\"javascript\" src=\"https://code.jquery.com/jquery-3.3.1.js\"></script>\n" +
                "\t<script type=\"text/javascript\" language=\"javascript\" src=\"https://cdn.datatables.net/1.10.19/js/jquery.dataTables.min.js\"></script>\n" +
                "\t<script type=\"text/javascript\" language=\"javascript\" src=\"../resources/demo.js\"></script>\n" +
                "\t<script type=\"text/javascript\" class=\"init\">\n" +
                "\t\n" +
                "\n" +
                "$(document).ready(function() {\n" +
                "\t$('#example').DataTable( {\n" +
                "\t\tcolumnDefs: [ {\n" +
                "\t\t\ttargets: [ 0 ],\n" +
                "\t\t\torderData: [ 0, 1 ]\n" +
                "\t\t}, {\n" +
                "\t\t\ttargets: [ 1 ],\n" +
                "\t\t\torderData: [ 1, 0 ]\n" +
                "\t\t}, {\n" +
                "\t\t\ttargets: [ 2 ],\n" +
                "\t\t\torderData: [ 1, 0 ]\n" +
                "\t\t} , {\"className\": \"dt-center\", \"targets\": \"_all\"} ]\n" +
                "\n" +
                "\t} );\n" +
                "} );\n" +
                "\n" +
                "\t</script>\n" +
                "</head>";

        html += "<body>\n";
        html += "<table id=\"example\">\n";
        html += "<thead><tr>\n<th>Topic</th>\n<th>Value</th>\n<th>Time</th>\n</tr></thead>\n";
        html += "<tbody>\n";
        for(String key : temporalBuffer.keySet()){
            ArrayList<PublishRecord> publishRecordArrayList = temporalBuffer.get(key);
            for (int i = 0;i<publishRecordArrayList.size(); i++){
                html += "<tr>\n";
                html += "<td>" + key + "</td>\n";
                html += "<td>" + publishRecordArrayList.get(i).getValue() + "</td>\n";
                html += "<td>" + sdf.format(publishRecordArrayList.get(i).getPublishDate()) + "</td>\n";
                html += "</tr>\n";
            }

        }
        html += "</tbody>\n";
        html += "</table>\n";
        html += "</body>\n";
        html += "</html>\n";
        return html;
    }

    public synchronized String lastValueBufferToHTML(){
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String html = "<html>\n";
        html += "<head>\n" +
                "\t<meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\">\n" +
                "\t<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,user-scalable=no\">\n" +
                "\t<title>DataTables example - Multi-column ordering</title>\n" +
                "\t<link rel=\"shortcut icon\" type=\"image/png\" href=\"/media/images/favicon.png\">\n" +
                "\t<link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS 2.0\" href=\"http://www.datatables.net/rss.xml\">\n" +
                "\t<link rel=\"stylesheet\" type=\"text/css\" href=\"/media/css/site-examples.css?_=19472395a2969da78c8a4c707e72123a\">\n" +
                "\t<link rel=\"stylesheet\" type=\"text/css\" href=\"https://cdn.datatables.net/1.10.19/css/jquery.dataTables.min.css\">\n" +
                "\t<style type=\"text/css\" class=\"init\">\n" +
                "\t\n" +
                "\t</style>\n" +
                "\t<script type=\"text/javascript\" async=\"\" src=\"https://ssl.google-analytics.com/ga.js\"></script><script type=\"text/javascript\" src=\"/media/js/site.js?_=5e8f232afab336abc1a1b65046a73460\"></script>\n" +
                "\t<script type=\"text/javascript\" src=\"/media/js/dynamic.php?comments-page=examples%2Fbasic_init%2Fmulti_col_sort.html\" async=\"\"></script>\n" +
                "\t<script type=\"text/javascript\" language=\"javascript\" src=\"https://code.jquery.com/jquery-3.3.1.js\"></script>\n" +
                "\t<script type=\"text/javascript\" language=\"javascript\" src=\"https://cdn.datatables.net/1.10.19/js/jquery.dataTables.min.js\"></script>\n" +
                "\t<script type=\"text/javascript\" language=\"javascript\" src=\"../resources/demo.js\"></script>\n" +
                "\t<script type=\"text/javascript\" class=\"init\">\n" +
                "\t\n" +
                "\n" +
                "$(document).ready(function() {\n" +
                "\t$('#example').DataTable( {\n" +
                "\t\tcolumnDefs: [ {\n" +
                "\t\t\ttargets: [ 0 ],\n" +
                "\t\t\torderData: [ 0, 1 ]\n" +
                "\t\t}, {\n" +
                "\t\t\ttargets: [ 1 ],\n" +
                "\t\t\torderData: [ 1, 0 ]\n" +
                "\t\t}, {\n" +
                "\t\t\ttargets: [ 2 ],\n" +
                "\t\t\torderData: [ 1, 0 ]\n" +
                "\t\t} , {\"className\": \"dt-center\", \"targets\": \"_all\"} ]\n" +
                "\n" +
                "\t} );\n" +
                "} );\n" +
                "\n" +
                "\t</script>\n" +
                "</head>";
        html += "<body>\n";
        html += "<table id=\"example\">\n";
        html += "<thead>\n<tr>\n<th>Topic</th>\n<th>Value</th>\n<th>Publish Date</th>\n<th>Expiration Date</th>\n";
        html += "<th>QuarterHourAVG</th><th>QuarterHourSUM</th>\n<th>QuarterHourCOUNT</th>\n<th>QuarterHourMAX</th>\n<th>QuarterHourMIN/th>\n";
        html += "<th>HourAVG</th><th>HourSUM</th>\n<th>HourCOUNT</th>\n<th>HourMAX</th>\n<th>HourMIN/th>\n";
        html += "<th>DailyAVG</th><th>DailySUM</th>\n<th>DailyCOUNT</th>\n<th>DailyMAX</th>\n<th>DailyMIN</th>\n</tr>\n</thead>\n";
        for(String key : lastValueBuffer.keySet()){
            LastValueRecord lastValueRecord = lastValueBuffer.get(key);
            html += "<tr>";
            html += "<td>"+ key + "</td>";
            html += "<td>"+ lastValueRecord.getPublishRecord().getValue() + "</td>";
            html += "<td>"+ sdf.format(lastValueRecord.getPublishRecord().getPublishDate()) + "</td>";
            html += "<td>"+ sdf.format(lastValueRecord.getExpirationDate()) + "</td>";
            html += "<td>"+ lastValueRecord.getStatistics().getQuarterHourStatistics().getAvg() + "</td>";
            html += "<td>"+ lastValueRecord.getStatistics().getQuarterHourStatistics().getSum() + "</td>";
            html += "<td>"+ lastValueRecord.getStatistics().getQuarterHourStatistics().getCount() + "</td>";
            html += "<td>"+ lastValueRecord.getStatistics().getQuarterHourStatistics().getMax()+ "</td>";
            html += "<td>"+ lastValueRecord.getStatistics().getQuarterHourStatistics().getMin()+ "</td>";
            html += "<td>"+ lastValueRecord.getStatistics().getHourStatistics().getAvg() + "</td>";
            html += "<td>"+ lastValueRecord.getStatistics().getHourStatistics().getSum() + "</td>";
            html += "<td>"+ lastValueRecord.getStatistics().getHourStatistics().getCount() + "</td>";
            html += "<td>"+ lastValueRecord.getStatistics().getHourStatistics().getMax()+ "</td>";
            html += "<td>"+ lastValueRecord.getStatistics().getHourStatistics().getMin()+ "</td>";
            html += "<td>"+ lastValueRecord.getStatistics().getDailyStatistics().getAvg() + "</td>";
            html += "<td>"+ lastValueRecord.getStatistics().getDailyStatistics().getSum() + "</td>";
            html += "<td>"+ lastValueRecord.getStatistics().getDailyStatistics().getCount() + "</td>";
            html += "<td>"+ lastValueRecord.getStatistics().getDailyStatistics().getMax()+ "</td>";
            html += "<td>"+ lastValueRecord.getStatistics().getDailyStatistics().getMin()+ "</td>";

            html += "</tr>";
        }
        html += "</table>\n";
        html += "</body>\n";
        html += "</html>\n";
        return html;
    }


}
