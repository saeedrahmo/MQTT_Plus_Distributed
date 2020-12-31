package MqttPlus.Utils;

import MqttPlus.JavaHTTPServer;
import MqttPlus.Routing.AdvertisementHandling;
import MqttPlus.Routing.ORT;
import MqttPlus.Routing.ServerState;
import org.json.simple.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class JSONUtility implements Runnable{

    private String topology;
    private int brokersNumber;
    private static JSONUtility instance;

    private JSONUtility(String topology, int brokersNumber){
        this.brokersNumber = brokersNumber;
        this.topology = topology;
    }

    public static JSONUtility getInstance(String topology, int brokersNumber){
        if(instance == null){
            instance = new JSONUtility(topology, brokersNumber);
        }
        return instance;
    }

    public void readTopology(String topology, int brokersNumber){
        String baseDir = "/home/parallels/MQTTPlusWS-master/src/topologies/";
        JSONParser jsonParser = new JSONParser();
        String finalDir = new String();

        switch (topology){
            case "star":
                finalDir = baseDir.concat("star/");
                break;

            case "linear":
                finalDir = baseDir.concat("linear/");
                break;

            case "tree":
                finalDir = baseDir.concat("tree/");
                break;
        }
        String fileDir = finalDir.concat(brokersNumber + ".json");
        try (FileReader reader = new FileReader(fileDir)) {
            Object obj = jsonParser.parse(reader);
            JSONArray hostList = (JSONArray) obj;

            for (int i = 0; i < hostList.size(); i++){
                JSONObject host = (JSONObject)hostList.get(i);
                if(host.get("hostname").equals(AdvertisementHandling.myHostname(JavaHTTPServer.local))){
                    JSONArray neighbours = (JSONArray) host.get("neighbours");
                    for (int j = 0; j < neighbours.size(); j++){
                        ORT.getInstance().insertHop((String) neighbours.get(j));

                    }
                }
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        synchronized (this) {
            readTopology(topology, brokersNumber);
            JavaHTTPServer.setState(ServerState.valueOf("NORMAL"));
            this.notifyAll();
        }
        System.out.println(ORT.getInstance().getOneHopBrokers());
    }
}
