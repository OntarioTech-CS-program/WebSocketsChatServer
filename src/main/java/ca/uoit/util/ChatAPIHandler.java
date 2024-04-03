package ca.uoit.util;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ChatAPIHandler {

    public static void saveChatRoomHistory(String roomID, String log) throws IOException {
        String uriAPI = "http://localhost:8080/ChatAPI-1.0-SNAPSHOT/api/history/" + roomID;
        URL url = new URL(uriAPI);
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        String jsonInputString = "{\"room\":\""+roomID+"\",\"log\":\""+log+"\"}";
        System.out.println(jsonInputString);
        try(OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try(BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            System.out.println(response.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    public static String loadChatRoomHistory(String roomID) throws IOException {
        String uriAPI = "http://localhost:8080/ChatAPI-1.0-SNAPSHOT/api/history/" + roomID;
        URL url = new URL(uriAPI);
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(false);
        con.setDoInput(true);

        InputStream inStream = con.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(inStream));

        StringBuffer buffer = new StringBuffer();
        String line;
        while ((line = in.readLine()) != null) {
            buffer.append(line);
        }
        String jsonData = buffer.toString();

        System.out.println("load the data");
        System.out.println(jsonData);

        JSONObject data = new JSONObject(jsonData);
        Map<String, Object> mapData = data.toMap();
        String content = (String) mapData.get("log");

        return content;

    }

}
