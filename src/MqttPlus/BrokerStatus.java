package MqttPlus;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class BrokerStatus {

    private static String path = "./brokerFunctions.json";

    public static String brokerFunctions(){

        JSONParser parser = new JSONParser();

        try {
            JSONArray object = (JSONArray) parser.parse(new FileReader(path));
            return object.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
