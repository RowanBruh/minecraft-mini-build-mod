package com.aicompanion.mod.web.handler;

import com.aicompanion.mod.AICompanionMod;
import com.aicompanion.mod.web.security.JWTManager;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import java.util.List;
import java.util.Map;

/**
 * Configuration for WebSocket endpoints
 * Handles authentication for WebSocket connections
 */
public class WebSocketHandlerConfig implements WebSocketCreator {
    
    @Override
    public Object createWebSocket(ServletUpgradeRequest request, ServletUpgradeResponse response) {
        // Get token from query parameter
        Map<String, List<String>> parameters = request.getParameterMap();
        boolean authenticated = false;
        
        if (parameters != null && parameters.containsKey("token")) {
            String token = parameters.get("token").get(0);
            
            // Verify token
            try {
                if (JWTManager.getInstance().isTokenValid(token)) {
                    // Token is valid, proceed with handshake
                    AICompanionMod.LOGGER.info("WebSocket connection authenticated");
                    authenticated = true;
                } else {
                    // Token is invalid, but we'll let the connection proceed
                    // and handle this in the onOpen method
                    AICompanionMod.LOGGER.warn("WebSocket connection with invalid token");
                }
            } catch (Exception e) {
                AICompanionMod.LOGGER.error("Error validating WebSocket token", e);
            }
        } else {
            // No token provided
            AICompanionMod.LOGGER.warn("WebSocket connection attempt without token");
        }
        
        // Store authentication status in the request
        request.setServletAttribute("authenticated", authenticated);
        
        // Create and return a new WebSocketHandler instance
        return new WebSocketHandler();
    }
}