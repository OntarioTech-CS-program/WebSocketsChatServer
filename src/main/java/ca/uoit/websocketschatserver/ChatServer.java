package ca.uoit.websocketschatserver;

import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.*;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static ca.uoit.util.ChatAPIHandler.loadChatRoomHistory;
import static ca.uoit.util.ChatAPIHandler.saveChatRoomHistory;

@ServerEndpoint(value="/ws/{roomID}")
public class ChatServer {

    private Map<String, String> usernames = new HashMap<String, String>();
    private static Map<String, String> roomList = new HashMap<String, String>();
    private static Map<String, String> roomHistoryList = new HashMap<String, String>();

    @OnOpen
    public void open(@PathParam("roomID") String roomID, Session session) throws IOException{
        roomList.put(session.getId(), roomID); // adding userID to a room
        // loading the chat history
        String history = loadChatRoomHistory(roomID);
        System.out.println("Room joined ");
        if (history!=null && !(history.isBlank())){
            System.out.println(history);
            history = history.replaceAll(System.lineSeparator(), "\\\n");
            System.out.println(history);
            session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\""+history+" \\n Chat room history loaded\"}");
            roomHistoryList.put(roomID, history+" \\n "+roomID + " room resumed.");
        }
        if(!roomHistoryList.containsKey(roomID)) { // only if this room has no history yet
            roomHistoryList.put(roomID, roomID + " room Created."); //initiating the room history
        }

        session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server ): Welcome to the chat room. Please state your username to begin.\"}");

    }

    @OnClose
    public void close(Session session) throws IOException{
        String userId = session.getId();
        if (usernames.containsKey(userId)) {
            String username = usernames.get(userId);
            usernames.remove(userId);
            String roomID = roomList.get(userId);
            roomList.remove(roomID);

            // adding event to the history of the room
            String logHistory = roomHistoryList.get(roomID);
            roomHistoryList.put(roomID, logHistory + "\\n " + username + " left the chat room.");

            //broadcast this person left the server
            int countPeers = 0;
            for (Session peer : session.getOpenSessions()){
                // only broadcast messages to those in the same room
                if(roomList.get(peer.getId()).equals(roomID)) {
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): " + username + " left the chat room.\"}");
                    countPeers++;
                }
            }
            // if everyone in the room left, save history
            if(!(countPeers > 0)){
                saveChatRoomHistory(roomID, roomHistoryList.get(roomID));
            }
        }
    }

    @OnMessage
    public void handleMessage(String comm, Session session) throws IOException{
        String userID = session.getId();
        String roomID = roomList.get(userID);
        JSONObject jsonmsg = new JSONObject(comm);
        String type = (String) jsonmsg.get("type");
        String message = (String) jsonmsg.get("msg");

        // not their first message
        if(usernames.containsKey(userID)){
            String username = usernames.get(userID);
            System.out.println(username);

            // adding event to the history of the room
            String logHistory = roomHistoryList.get(roomID);
            roomHistoryList.put(roomID, logHistory + "\\n " +"(" + username + "): " + message);

            for(Session peer: session.getOpenSessions()){
                // only broadcast messages to those in the same room
                if(roomList.get(peer.getId()).equals(roomID)) {
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(" + username + "): " + message + "\"}");
                }
            }
        }else{ //first message is their username
            usernames.put(userID, message);
            session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): Welcome, " + message + "!\"}");

            // adding event to the history of the room
            String logHistory = roomHistoryList.get(roomID);
            roomHistoryList.put(roomID, logHistory+"\\n " + message + " joined the chat room.");

            //broadcast this person joined the server to the rest
            for(Session peer: session.getOpenSessions()){
                if((!peer.getId().equals(userID)) && (roomList.get(peer.getId()).equals(roomID))){
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): " + message + " joined the chat room.\"}");
                }
            }
        }

    }

}
