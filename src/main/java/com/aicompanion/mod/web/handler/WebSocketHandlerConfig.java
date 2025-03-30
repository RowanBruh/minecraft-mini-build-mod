package com.aicompanion.mod.web.handler;

import com.aicompanion.mod.AICompanionMod;
import com.aicompanion.mod.web.security.JWTManager;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for WebSocket endpoints
 * Handles authentication for WebSocket connections
 */
public class WebSocketHandlerConfig implements WebSocketCreator {
    
    // Map to store session authentication status
    private static final Map<String, Boolean> authenticatedSessions = new HashMap<>();
    
    /**
     * Check if a session is authenticated
     */
    public static boolean isSessionAuthenticated(Session session) {
        String sessionId = session.getRemote().toString();
        return authenticatedSessions.getOrDefault(sessionId, false);
    }
    
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
        
        // Create and return a new WebSocketHandler instance
        // Authentication will be handled in WebSocketHandler.onOpen by checking the token directly
        return new WebSocketHandler();
    }
    
    /**
     * Updates the authentication status for a session after it's been created
     * Called from WebSocketHandler.onOpen
     */
    public static void setSessionAuthenticated(Session session, boolean authenticated) {
        String sessionId = session.getRemote().toString();
        authenticatedSessions.put(sessionId, authenticated);
    }
}