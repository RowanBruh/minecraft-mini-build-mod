package com.aicompanion.mod.web.handler;

import com.aicompanion.mod.AICompanionMod;
import com.aicompanion.mod.web.WebServer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.websocket.*;
import javax.websocket.CloseReason;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.UUID;

/**
 * WebSocket handler for real-time communication with the web interface
 */
@ServerEndpoint(value = "/ws", configurator = WebSocketHandlerConfig.class)
public class WebSocketHandler {
    private static final Gson gson = new Gson();
    private Session session;
    private UUID clientId;

    /**
     * Handle new WebSocket connection
     */
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        this.clientId = UUID.randomUUID();
        
        // Check if the session is authenticated
        Boolean authenticated = (Boolean) session.getUserProperties().get("authenticated");
        if (authenticated != null && !authenticated) {
            // Not authenticated, close the connection
            try {
                JsonObject message = new JsonObject();
                message.addProperty("type", "error");
                message.addProperty("message", "Authentication failed");
                session.getBasicRemote().sendText(gson.toJson(message));
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Authentication failed"));
                return;
            } catch (IOException e) {
                AICompanionMod.LOGGER.error("Error closing unauthenticated WebSocket", e);
            }
            return;
        }
        
        // Register client with the web server
        WebServer.getInstance().addClient(clientId, new WebSocketSessionAdapter(session));
        
        // Send welcome message
        JsonObject message = new JsonObject();
        message.addProperty("type", "connection");
        message.addProperty("clientId", clientId.toString());
        message.addProperty("message", "Connected to AI Companion WebSocket server");
        
        try {
            session.getBasicRemote().sendText(gson.toJson(message));
        } catch (IOException e) {
            AICompanionMod.LOGGER.error("Error sending welcome message", e);
        }
    }

    /**
     * Handle WebSocket message
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            String type = json.get("type").getAsString();
            
            switch (type) {
                case "ping":
                    handlePing(json);
                    break;
                    
                case "command":
                    handleCommand(json);
                    break;
                    
                default:
                    sendError("Unknown message type: " + type);
                    break;
            }
        } catch (Exception e) {
            sendError("Error processing message: " + e.getMessage());
            AICompanionMod.LOGGER.error("Error processing WebSocket message", e);
        }
    }

    /**
     * Handle WebSocket close
     */
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        // Unregister client from the web server
        WebServer.getInstance().removeClient(clientId);
        AICompanionMod.LOGGER.info("WebSocket connection closed: " + closeReason.getReasonPhrase());
    }

    /**
     * Handle WebSocket error
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        AICompanionMod.LOGGER.error("WebSocket error", throwable);
    }

    /**
     * Handle ping message
     */
    private void handlePing(JsonObject json) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "pong");
        response.addProperty("timestamp", System.currentTimeMillis());
        
        try {
            session.getBasicRemote().sendText(gson.toJson(response));
        } catch (IOException e) {
            AICompanionMod.LOGGER.error("Error sending pong message", e);
        }
    }

    /**
     * Handle command message
     */
    private void handleCommand(JsonObject json) {
        // TODO: Process commands
        // This would be implemented based on your command system
        
        String command = json.get("command").getAsString();
        AICompanionMod.LOGGER.info("WebSocket command received: " + command);
        
        // Send acknowledgment
        JsonObject response = new JsonObject();
        response.addProperty("type", "command_ack");
        response.addProperty("command", command);
        response.addProperty("status", "received");
        
        try {
            session.getBasicRemote().sendText(gson.toJson(response));
        } catch (IOException e) {
            AICompanionMod.LOGGER.error("Error sending command acknowledgment", e);
        }
    }

    /**
     * Send error message to client
     */
    private void sendError(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "error");
        response.addProperty("message", message);
        
        try {
            session.getBasicRemote().sendText(gson.toJson(response));
        } catch (IOException e) {
            AICompanionMod.LOGGER.error("Error sending error message", e);
        }
    }

    /**
     * Adapter class to convert Session to WebSocketSession
     */
    private static class WebSocketSessionAdapter implements WebServer.WebSocketSession {
        private final Session session;
        
        public WebSocketSessionAdapter(Session session) {
            this.session = session;
        }
        
        @Override
        public boolean isOpen() {
            return session.isOpen();
        }
        
        @Override
        public void sendMessage(String message) throws IOException {
            session.getBasicRemote().sendText(message);
        }
    }
}