package MqttPlus.Handlers;

import MqttPlus.*;
import MqttPlus.ImageAnalysis.ImageAnalyzer;
import MqttPlus.ImageAnalysis.ImageAnalyzerException;
import MqttPlus.Publish.Publish;
import MqttPlus.PublishBuffers.*;
import MqttPlus.Routing.*;
import MqttPlus.SubscriptionBuffer.SubscriptionBuffer;
import MqttPlus.Utils.MQTTPublish;
import MqttPlus.enums.DataType;
import MqttPlus.enums.InformationExtractionOperatorEnum;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.json.JSONObject;

import java.sql.SQLOutput;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Base64;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class PublishHandler {


    private static PublishHandler instance;

    private PublishHandler(){
    }

    public static PublishHandler getInstance(){
        if(instance==null){
            instance = new PublishHandler();
        }
        return instance;
    }


    public JSONObject handlePublish(JSONObject obj){
        SubscriptionBuffer subscriptionBuffer = SubscriptionBuffer.getInstance();

        Publish publish = Publish.parsePublish(obj);


        if(JavaHTTPServer.distributedProtocol) {
            if (publish.getTopic().contains("@")) {
                String hostname = publish.getTopic().split("@")[1];
                String cleanTopic = publish.getTopic().split("@")[0];
                MQTTPublish.sendPublish(cleanTopic, publish.getPayload());

                SRT subscriptionRoutingTable = SRT.getInstance();
                subscriptionRoutingTable.insertTopic(cleanTopic, hostname);

                AdvertisementHandling.forward(cleanTopic, publish.getPayload(), JavaHTTPServer.local, hostname);
                publish.setTopic(cleanTopic);

                    if (!SRT.getInstance().isSubAdvSent(cleanTopic, hostname)) {
                        for (String sub : SRT.getInstance().findSubAdv(cleanTopic, hostname)) {
                            SRT.getInstance().recordSubscriptionAdvSent(sub, hostname);
                            SubscriptionHandler.getInstance().sendSubscription(sub, hostname);
                        }
                }

            } else if (!AdvertisementHandling.isAlreadyPublished(publish.getTopic()) && !PRT.getInstance().findTopic(publish.getTopic()) && !publish.getClientId().contains("PRT")) {
                AdvertisementHandling.addTopic(publish.getTopic());
                AdvertisementHandling.advertise(publish.getTopic(), publish.getPayload(), JavaHTTPServer.local);
            } else if (PRT.getInstance().findTopic(publish.getTopic())) {
                System.out.println("PRT: " + PRT.getInstance());
                System.out.println("SRT: " + SRT.getInstance());
                System.out.println("ORT: " + ORT.getInstance());
                PRT.getInstance().forwardPublish(publish);
            }
        }
        System.out.println("PROVA COMPUTEDURATION");
        computeDuration(publish.getPayload().toString().split("timestamp: ")[1].substring(1, 16));
        System.out.println("TEST USCITA");

        DataType inputDT = getInputDT(publish.getPayload());

        PublishBuffer publishBuffer = null;

        if(inputDT==DataType.NUMERIC){
            publishBuffer = NumericBuffer.getInstance();
            publishBuffer.insertLastValue(publish.getTopic(),new Double(publish.getPayload().toString()));
            publishBuffer.insertTemporalValue(publish.getTopic(),new Double(publish.getPayload().toString()));
            System.out.println(publishBuffer.temporalValueBufferToString());
        }
        else if(inputDT == DataType.IMAGEBUFFER){
            ImageAnalyzer imageAnalyzer = null;
            if(subscriptionBuffer.existMatchingSubscription(publish.getTopic(), InformationExtractionOperatorEnum.COUNTPEOPLE)){
                publishBuffer = CountPeopleBuffer.getInstance();
                int peopleCount;
                try {
                    if(imageAnalyzer == null){
                        imageAnalyzer = new ImageAnalyzer((String)publish.getPayload());
                    }
                    peopleCount = imageAnalyzer.countPeople((String)publish.getPayload());
                    publishBuffer.insertLastValue(publish.getTopic(),new Double(peopleCount));
                    publishBuffer.insertTemporalValue(publish.getTopic(),new Double((peopleCount)));
                } catch (ImageAnalyzerException e) {
                    e.printStackTrace();
                    MQTTPublish.sendPublish(publish.getTopic(),e.getMessage());
                }
            }
            if(subscriptionBuffer.existMatchingSubscription(publish.getTopic(), InformationExtractionOperatorEnum.COUNTMALE)){
                publishBuffer = CountMaleBuffer.getInstance();
                int maleCount;
                try {
                    if(imageAnalyzer == null){
                        imageAnalyzer = new ImageAnalyzer((String)publish.getPayload());
                    }
                    maleCount = imageAnalyzer.countMale((String) publish.getPayload());
                    publishBuffer.insertLastValue(publish.getTopic(),new Double(maleCount));
                    publishBuffer.insertTemporalValue(publish.getTopic(),new Double((maleCount)));
                } catch (ImageAnalyzerException e) {
                    e.printStackTrace();
                    MQTTPublish.sendPublish(publish.getTopic(),e.getMessage());
                }
            }
            if((subscriptionBuffer.existMatchingSubscription(publish.getTopic(), InformationExtractionOperatorEnum.COUNTFEMALE))){
                publishBuffer = CountFemaleBuffer.getInstance();
                int femaleCount;
                try {
                    if(imageAnalyzer == null){
                        imageAnalyzer = new ImageAnalyzer((String)publish.getPayload());
                    }
                    femaleCount = imageAnalyzer.countFemale((String)publish.getPayload());
                    publishBuffer.insertLastValue(publish.getTopic(),new Double(femaleCount));
                    publishBuffer.insertTemporalValue(publish.getTopic(),new Double((femaleCount)));
                } catch (ImageAnalyzerException e) {
                    e.printStackTrace();
                    MQTTPublish.sendPublish(publish.getTopic(),e.getMessage());
                }

            }
        }

        HashMap<String,Object> hashMap = PublishVirtualizer.rewrittenPublishes(publish,publishBuffer);
        for(String key : hashMap.keySet()){
            MQTTPublish.sendPublish(key,hashMap.get(key));
        }

        return new JSONObject(hashMap);

    }

    public DataType getInputDT(Object objPayload){

        try {
            new Double(objPayload.toString());
            return DataType.NUMERIC;
        }
        catch (NumberFormatException e){
            try {


                String imageString =((String)objPayload);
                // create a buffered image
                BufferedImage image = null;
                byte[] imageByte;

                imageByte = Base64.getDecoder().decode(imageString);
                ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
                image = ImageIO.read(bis);
                bis.close();

                if(image!=null) {
                    File outputFile = new File(ImageAnalyzer.path + "image.jpeg");
                    ImageIO.write(image,"jpeg",outputFile);
                    return DataType.IMAGEBUFFER;
                }
            }
            catch (IOException e1) {

            }
            catch (ClassCastException e1){

            }
        }
        return DataType.STRING;


    }

    private void computeDuration(String startingTimeStamp){
        System.out.println("Dentro computeDuration");
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSSSSS");
        try {
            Date d1 = format.parse(Instant.now().toString().split("T")[1].split("Z")[0].substring(0, 15));
            Date d2 = format.parse(startingTimeStamp);
            long diff = d1.getTime() - d2.getTime();
            long diffSeconds = diff / 1000 % 60;

            System.out.println("Packet processing time:" + diffSeconds);
        } catch (ParseException e) {
            if (startingTimeStamp.equals(""))
                return;
        }
    }





}
