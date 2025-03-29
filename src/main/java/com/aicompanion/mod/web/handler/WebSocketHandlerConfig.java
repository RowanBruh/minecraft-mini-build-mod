package com.aicompanion.mod.web.handler;

import com.aicompanion.mod.AICompanionMod;
import com.aicompanion.mod.web.security.JWTManager;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.util.List;
import java.util.Map;

/**
 * Configuration for WebSocket endpoints
 * Handles authentication for WebSocket connections
 */
public class WebSocketHandlerConfig extends ServerEndpointConfig.Configurator {
    
    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        // Get token from query parameter
        Map<String, List<String>> parameters = request.getParameterMap();
        
        if (parameters.containsKey("token")) {
            String token = parameters.get("token").get(0);
            
            // Verify token
            try {
                if (JWTManager.getInstance().isTokenValid(token)) {
                    // Token is valid, proceed with handshake
                    AICompanionMod.LOGGER.info("WebSocket connection authenticated");
                    sec.getUserProperties().put("authenticated", true);
                } else {
                    // Token is invalid, but we'll let the connection proceed
                    // and handle this in the onOpen method
                    AICompanionMod.LOGGER.warn("WebSocket connection with invalid token");
                    sec.getUserProperties().put("authenticated", false);
                }
            } catch (Exception e) {
                AICompanionMod.LOGGER.error("Error validating WebSocket token", e);
                sec.getUserProperties().put("authenticated", false);
            }
        } else {
            // No token provided
            AICompanionMod.LOGGER.warn("WebSocket connection attempt without token");
            sec.getUserProperties().put("authenticated", false);
        }
        
        super.modifyHandshake(sec, request, response);
    }
}