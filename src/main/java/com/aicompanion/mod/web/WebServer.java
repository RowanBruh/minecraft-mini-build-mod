package com.aicompanion.mod.web;

import com.aicompanion.mod.AICompanionMod;
import com.aicompanion.mod.config.AICompanionConfig;
import com.aicompanion.mod.entity.AICompanionEntity;
import com.aicompanion.mod.web.handler.APIHandler;
import com.aicompanion.mod.web.handler.AuthHandler;
import com.aicompanion.mod.web.handler.StaticFileHandler;
import com.aicompanion.mod.web.handler.WebSocketHandler;
import com.aicompanion.mod.web.security.JWTManager;
import io.jsonwebtoken.security.Keys;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URL;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Web server implementation for the AI Companion mod.
 * This handles the REST API, WebSocket connections, and static file serving.
 */
public class WebServer {
    private static WebServer instance;
    private Server server;
    private final int port;
    private boolean running = false;
    private JWTManager jwtManager;
    
    // Connected clients (UUID to websocket session)
    private final Map<UUID, WebSocketSession> connectedClients = new ConcurrentHashMap<>();
    
    // Executor for background tasks
    private final ScheduledExecutorService executor;

    /**
     * Private constructor - use getInstance() instead
     */
    private WebServer() {
        // Get port from config
        this.port = AICompanionConfig.SERVER.webInterfacePort.get();
        
        // Initialize the executor
        this.executor = Executors.newScheduledThreadPool(2);
        
        // Generate JWT secret key if not present
        String secretKey = AICompanionConfig.SERVER.webInterfaceSecretKey.get();
        if (secretKey == null || secretKey.isEmpty()) {
            SecureRandom random = new SecureRandom();
            byte[] keyBytes = new byte[64];
            random.nextBytes(keyBytes);
            secretKey = Base64.getEncoder().encodeToString(keyBytes);
            AICompanionConfig.SERVER.webInterfaceSecretKey.set(secretKey);
        }
        
        // Initialize the JWT manager
        Key key = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.jwtManager = new JWTManager(key);
    }

    /**
     * Get the singleton instance of the web server
     */
    public static synchronized WebServer getInstance() {
        if (instance == null) {
            instance = new WebServer();
        }
        return instance;
    }

    /**
     * Start the web server
     */
    public synchronized void start() {
        if (running) {
            return;
        }
        
        try {
            // Create the server
            server = new Server(port);
            
            // Set up context handlers
            ContextHandlerCollection contexts = new ContextHandlerCollection();
            
            // API context
            ServletContextHandler apiContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
            apiContext.setContextPath("/api");
            
            // Auth servlet (handles login)
            apiContext.addServlet(new ServletHolder(new AuthHandler(jwtManager)), "/auth/*");
            
            // API servlet (protected by JWT)
            apiContext.addServlet(new ServletHolder(new APIHandler(this, jwtManager)), "/*");
            
            // Static files context
            ServletContextHandler staticContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
            staticContext.setContextPath("/");
            
            // Set resource base for static files
            URL baseURL = WebServer.class.getResource("/assets/aicompanion/web");
            if (baseURL != null) {
                staticContext.setBaseResource(Resource.newResource(baseURL));
                
                // Add servlet for static files
                ServletHolder holderDefault = new ServletHolder("default", DefaultServlet.class);
                holderDefault.setInitParameter("dirAllowed", "false");
                staticContext.addServlet(holderDefault, "/");
            } else {
                AICompanionMod.LOGGER.error("Could not find web interface static resources");
            }
            
            // Add WebSocket support
            try {
                WebSocketServerContainerInitializer.configure(staticContext, 
                        (servletContext, wsContainer) -> {
                    wsContainer.addEndpoint(WebSocketHandler.class);
                });
            } catch (ServletException e) {
                AICompanionMod.LOGGER.error("Failed to add WebSocket support", e);
            }
            
            // Add contexts to server
            contexts.addHandler(apiContext);
            contexts.addHandler(staticContext);
            server.setHandler(contexts);
            
            // Start the server
            server.start();
            running = true;
            
            // Schedule status broadcast
            executor.scheduleAtFixedRate(this::broadcastStatus, 5, 5, TimeUnit.SECONDS);
            
            AICompanionMod.LOGGER.info("Web server started on port " + port);
        } catch (Exception e) {
            AICompanionMod.LOGGER.error("Error starting web server", e);
        }
    }

    /**
     * Stop the web server
     */
    public synchronized void stop() {
        if (!running) {
            return;
        }
        
        try {
            executor.shutdown();
            
            if (server != null) {
                server.stop();
                server = null;
            }
            
            running = false;
            AICompanionMod.LOGGER.info("Web server stopped");
        } catch (Exception e) {
            AICompanionMod.LOGGER.error("Error stopping web server", e);
        }
    }

    /**
     * Register a new client connection
     */
    public void addClient(UUID clientId, WebSocketSession session) {
        connectedClients.put(clientId, session);
        AICompanionMod.LOGGER.info("Client connected: " + clientId);
    }

    /**
     * Remove a client connection
     */
    public void removeClient(UUID clientId) {
        connectedClients.remove(clientId);
        AICompanionMod.LOGGER.info("Client disconnected: " + clientId);
    }

    /**
     * Send a message to a specific client
     */
    public void sendMessage(UUID clientId, String message) {
        WebSocketSession session = connectedClients.get(clientId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(message);
            } catch (IOException e) {
                AICompanionMod.LOGGER.error("Error sending message to client " + clientId, e);
            }
        }
    }

    /**
     * Send a message to all connected clients
     */
    public void broadcastMessage(String message) {
        for (Map.Entry<UUID, WebSocketSession> entry : connectedClients.entrySet()) {
            if (entry.getValue().isOpen()) {
                try {
                    entry.getValue().sendMessage(message);
                } catch (IOException e) {
                    AICompanionMod.LOGGER.error("Error broadcasting message to client " + entry.getKey(), e);
                }
            }
        }
    }
    
    /**
     * Broadcast status updates to all connected clients
     */
    private void broadcastStatus() {
        // TODO: Collect status of all AI companions and broadcast
    }
    
    /**
     * WebSocket session interface to abstract the actual implementation
     */
    public interface WebSocketSession {
        boolean isOpen();
        void sendMessage(String message) throws IOException;
    }
}