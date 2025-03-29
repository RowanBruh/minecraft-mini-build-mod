package com.aicompanion.mod.web.handler;

import com.aicompanion.mod.AICompanionMod;
import com.aicompanion.mod.config.AICompanionConfig;
import com.aicompanion.mod.web.security.JWTManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * Handles authentication requests for the web interface
 */
public class AuthHandler extends HttpServlet {
    private final JWTManager jwtManager;
    private final Gson gson = new Gson();

    public AuthHandler(JWTManager jwtManager) {
        this.jwtManager = jwtManager;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        
        if ("/login".equals(path)) {
            handleLogin(req, resp);
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"error\": \"Unknown endpoint\"}");
        }
    }

    /**
     * Handle login requests
     */
    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Read the request body
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = req.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        
        try {
            // Parse the JSON
            JsonObject json = gson.fromJson(sb.toString(), JsonObject.class);
            String username = json.get("username").getAsString();
            String password = json.get("password").getAsString();
            
            // Check credentials against config
            String configUsername = AICompanionConfig.SERVER.webInterfaceUsername.get();
            String configPassword = AICompanionConfig.SERVER.webInterfacePassword.get();
            
            if (username.equals(configUsername) && password.equals(configPassword)) {
                // Generate token
                String token = jwtManager.generateToken(username);
                
                // Return token
                resp.setContentType("application/json");
                resp.setStatus(HttpServletResponse.SC_OK);
                JsonObject response = new JsonObject();
                response.addProperty("token", token);
                response.addProperty("username", username);
                resp.getWriter().write(gson.toJson(response));
                
                AICompanionMod.LOGGER.info("User " + username + " logged in successfully");
            } else {
                // Invalid credentials
                resp.setContentType("application/json");
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                JsonObject response = new JsonObject();
                response.addProperty("error", "Invalid credentials");
                resp.getWriter().write(gson.toJson(response));
                
                AICompanionMod.LOGGER.info("Failed login attempt for user " + username);
            }
        } catch (Exception e) {
            // Invalid JSON or missing fields
            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject response = new JsonObject();
            response.addProperty("error", "Invalid request format");
            resp.getWriter().write(gson.toJson(response));
            
            AICompanionMod.LOGGER.error("Error processing login request", e);
        }
    }
}