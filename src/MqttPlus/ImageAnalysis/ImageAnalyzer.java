package MqttPlus.ImageAnalysis;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class ImageAnalyzer {

    public static String path = "/home/luca/FaceDetect/";
    private static final String SERVER_URL = "https://api-us.faceplusplus.com/facepp/v3/detect";
    private static final String API_KEY_STRING = "api_key";
    private static final String API_SECRET_STRING = "api_secret";
    private static final String IMAGE_STRNG = "image_base64";
    private static final String GENDER_STRING = "gender";
    private static final String RETURN_ATTRIBUTES_STRING = "return_attributes";
    private static final String API_KEY = "rgKqhBFRBQhZA1K2uvZk64kDnvFq3BzN";
    private static final String API_SECRET = "3fFB-IPWbSSn6BU4l_H7w-PoK10VK0JT";
    private static final int CONNECT_TIME_OUT = 30000;
    private static final int READ_OUT_TIME = 50000;
    private String boundaryString = getBoundary();

    private JSONObject result;

    public ImageAnalyzer(String base64Image) throws ImageAnalyzerException{
        HashMap<String,String> map = new HashMap<>();
        map.put(API_KEY_STRING,API_KEY);
        map.put(API_SECRET_STRING,API_SECRET);
        map.put(IMAGE_STRNG, base64Image);
        map.put(RETURN_ATTRIBUTES_STRING,GENDER_STRING);
        result = doPost(map);
        if (result == null) throw new ImageAnalyzerException();
    }

    public static int countPeoplePython(){
        int countPeople = 0;
        try {

            File dir = new File(path);
            System.out.println("count people");
            String command = "python count_people.py image.jpeg";
            System.out.println(command);
            Process process = Runtime.getRuntime().exec(command,null, dir);

            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String ret;
            while ((ret = in.readLine())!=null){
                if(ret.contains("faces")){
                    countPeople = Integer.valueOf(ret.split(" ")[1]);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Number of people : "+countPeople);
        return countPeople;
    }

    public int countPeople(String base64Image) throws ImageAnalyzerException{
        int peopleCount = 0;
        try {
            System.out.println("Response obj: \n" + result);
            peopleCount = result.getJSONArray("faces").length();

        } catch (Exception e) {
            e.printStackTrace();
            throw new ImageAnalyzerException();
        }
        return peopleCount;
    }

    private int genderCounter(String base64Image, String gender) throws ImageAnalyzerException{
        int count = 0;
        try {
            JSONArray responseArray = result.getJSONArray("faces");
            System.out.println("Response obj: \n" + result);
            for(int i = 0;i<responseArray.length();i++){
                JSONObject current = responseArray.getJSONObject(i);
                if(current.getJSONObject("attributes").getJSONObject("gender").getString("value").equals(gender)){
                    count++;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new ImageAnalyzerException();
        }
        return count;
    }

    public int countMale(String base64Image) throws ImageAnalyzerException{
        return genderCounter(base64Image, "Male");
    }
    public int countFemale(String base64Image) throws ImageAnalyzerException{
        return genderCounter(base64Image,"Female");
    }

    private JSONObject doPost(HashMap<String,String> map){
        HttpURLConnection connection;
        URL url;
        try {
            url = new URL(SERVER_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIME_OUT);
            connection.setReadTimeout(READ_OUT_TIME);
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundaryString);
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible;MSIE 6.0;Windows NT 5.1;SV1)");
            DataOutputStream obos = new DataOutputStream(connection.getOutputStream());
            Iterator iter = map.entrySet().iterator();
            while(iter.hasNext()){
                Map.Entry<String, String> entry = (Map.Entry) iter.next();
                String key = entry.getKey();
                String value = entry.getValue();
                obos.writeBytes("--" + boundaryString + "\r\n");
                obos.writeBytes("Content-Disposition: form-data; name=\"" + key
                        + "\"\r\n");
                obos.writeBytes("\r\n");
                obos.writeBytes(value + "\r\n");
            }
            obos.writeBytes("--" + boundaryString + "--" + "\r\n");
            obos.writeBytes("\r\n");
            obos.flush();
            obos.close();
            InputStream ins = null;
            int code = connection.getResponseCode();
            try{
                if(code == 200){
                    ins = connection.getInputStream();
                }else{
                    ins = connection.getErrorStream();
                }
            }catch (SSLException e){
                e.printStackTrace();
                return null;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buff = new byte[4096];
            int len;
            while((len = ins.read(buff)) != -1){
                baos.write(buff, 0, len);
            }
            byte[] bytes = baos.toByteArray();
            ins.close();
            return new JSONObject(new String(bytes));
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (ProtocolException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

    }

    private String getBoundary() {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for(int i = 0; i < 32; ++i) {
            sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-".charAt(random.nextInt("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_".length())));
        }
        return sb.toString();
    }


}
