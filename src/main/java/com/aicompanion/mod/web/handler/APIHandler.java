package com.aicompanion.mod.web.handler;

import com.aicompanion.mod.AICompanionMod;
import com.aicompanion.mod.entity.AICompanionEntity;
import com.aicompanion.mod.web.WebServer;
import com.aicompanion.mod.web.security.JWTManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles REST API requests for the web interface
 */
public class APIHandler extends HttpServlet {
    private final WebServer webServer;
    private final JWTManager jwtManager;
    private final Gson gson = new Gson();

    public APIHandler(WebServer webServer, JWTManager jwtManager) {
        this.webServer = webServer;
        this.jwtManager = jwtManager;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Set CORS headers for development
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        
        // Handle preflight requests
        if (req.getMethod().equals("OPTIONS")) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        
        // Authenticate request
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"error\": \"Authentication required\"}");
            return;
        }
        
        String token = authHeader.substring(7);
        try {
            Claims claims = jwtManager.validateToken(token);
            // Store username in request attributes
            req.setAttribute("username", claims.getSubject());
        } catch (JwtException e) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"error\": \"Invalid or expired token\"}");
            return;
        }
        
        // Continue with actual request
        super.service(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        
        if (path == null || path.equals("/")) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("{\"message\": \"AI Companion API\"}");
        } else if (path.equals("/companions")) {
            handleGetCompanions(req, resp);
        } else if (path.startsWith("/companions/")) {
            String companionId = path.substring("/companions/".length());
            handleGetCompanion(req, resp, companionId);
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"error\": \"Unknown endpoint\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        
        if (path == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"error\": \"Unknown endpoint\"}");
            return;
        }
        
        if (path.startsWith("/companions/") && path.endsWith("/command")) {
            String companionId = path.substring("/companions/".length(), path.length() - "/command".length());
            handleCompanionCommand(req, resp, companionId);
        } else if (path.startsWith("/companions/") && path.endsWith("/skin")) {
            String companionId = path.substring("/companions/".length(), path.length() - "/skin".length());
            handleCompanionSkinUpload(req, resp, companionId);
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"error\": \"Unknown endpoint\"}");
        }
    }

    /**
     * Handle GET request for all companions
     */
    private void handleGetCompanions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            resp.getWriter().write("{\"error\": \"Server not available\"}");
            return;
        }
        
        JsonArray companionsArray = new JsonArray();
        
        for (ServerWorld world : server.getAllLevels()) {
            List<AICompanionEntity> companions = world.getEntitiesOfClass(
                AICompanionEntity.class, 
                new AxisAlignedBB(
                    Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE,
                    Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE
                )
            );
            
            for (AICompanionEntity companion : companions) {
                JsonObject companionObj = createCompanionJson(companion);
                companionsArray.add(companionObj);
            }
        }
        
        JsonObject response = new JsonObject();
        response.add("companions", companionsArray);
        
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(gson.toJson(response));
    }

    /**
     * Handle GET request for a specific companion
     */
    private void handleGetCompanion(HttpServletRequest req, HttpServletResponse resp, String companionId) throws IOException {
        try {
            UUID uuid = UUID.fromString(companionId);
            AICompanionEntity companion = findCompanionById(uuid);
            
            if (companion == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\": \"Companion not found\"}");
                return;
            }
            
            JsonObject companionObj = createCompanionJson(companion);
            
            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(companionObj));
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Invalid companion ID format\"}");
        }
    }

    /**
     * Handle POST request to send a command to a companion
     */
    private void handleCompanionCommand(HttpServletRequest req, HttpServletResponse resp, String companionId) throws IOException {
        try {
            UUID uuid = UUID.fromString(companionId);
            AICompanionEntity companion = findCompanionById(uuid);
            
            if (companion == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\": \"Companion not found\"}");
                return;
            }
            
            // Read request body
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader reader = req.getReader()) {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            
            JsonObject json = gson.fromJson(sb.toString(), JsonObject.class);
            if (!json.has("command")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Missing command parameter\"}");
                return;
            }
            
            String command = json.get("command").getAsString();
            
            // TODO: Process command for companion
            // This will need to be implemented based on your companion command system
            
            // For now, just log it
            AICompanionMod.LOGGER.info("Web interface command for companion " + companionId + ": " + command);
            
            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("{\"success\": true, \"message\": \"Command sent\"}");
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Invalid companion ID format\"}");
        }
    }

    /**
     * Create a JSON representation of a companion
     */
    private JsonObject createCompanionJson(AICompanionEntity companion) {
        JsonObject companionObj = new JsonObject();
        companionObj.addProperty("id", companion.getUUID().toString());
        companionObj.addProperty("name", companion.getName().getString());
        
        // Owner info
        if (companion.getOwnerUUID() != null) {
            companionObj.addProperty("ownerUuid", companion.getOwnerUUID().toString());
            companionObj.addProperty("ownerName", companion.getOwnerName());
        }
        
        // Position
        JsonObject position = new JsonObject();
        position.addProperty("x", companion.getX());
        position.addProperty("y", companion.getY());
        position.addProperty("z", companion.getZ());
        companionObj.add("position", position);
        
        // Health
        companionObj.addProperty("health", companion.getHealth());
        companionObj.addProperty("maxHealth", companion.getMaxHealth());
        
        // Status
        // TODO: Add more detailed status information
        
        return companionObj;
    }

    /**
     * Handle POST request to upload a custom skin for a companion
     */
    private void handleCompanionSkinUpload(HttpServletRequest req, HttpServletResponse resp, String companionId) throws IOException {
        try {
            // Validate companion exists
            UUID uuid = UUID.fromString(companionId);
            AICompanionEntity companion = findCompanionById(uuid);
            
            if (companion == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\": \"Companion not found\"}");
                return;
            }
            
            // Check if the request is multipart
            boolean isMultipart = ServletFileUpload.isMultipartContent(req);
            
            if (!isMultipart) {
                // If not multipart, read the skinType parameter from JSON body
                StringBuilder sb = new StringBuilder();
                String line;
                try (BufferedReader reader = req.getReader()) {
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
                
                JsonObject json = gson.fromJson(sb.toString(), JsonObject.class);
                if (json != null && json.has("skinType")) {
                    String skinType = json.get("skinType").getAsString();
                    // Apply default skin
                    String skinPath = applySkin(companion, skinType, null);
                    
                    // Return success response with skin info
                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.addProperty("message", "Skin updated to " + skinType);
                    response.addProperty("skinType", skinType);
                    if (skinPath != null) {
                        response.addProperty("skinPath", skinPath);
                    }
                    
                    resp.setContentType("application/json");
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(gson.toJson(response));
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("{\"error\": \"Missing skinType parameter\"}");
                }
                return;
            }
            
            // Handle multipart form data (file upload)
            try {
                // Create a factory for disk-based file items
                DiskFileItemFactory factory = new DiskFileItemFactory();
                
                // Create a new file upload handler
                ServletFileUpload upload = new ServletFileUpload(factory);
                
                // Parse the request
                List<FileItem> items = upload.parseRequest(req);
                
                // Process the uploaded items
                String skinType = "default";
                File uploadedFile = null;
                
                for (FileItem item : items) {
                    if (item.isFormField()) {
                        // Process regular form field
                        if ("skinType".equals(item.getFieldName())) {
                            skinType = item.getString();
                        }
                    } else {
                        // Process file upload
                        if ("skin".equals(item.getFieldName()) && item.getSize() > 0) {
                            // Create the skins directory if it doesn't exist
                            File skinDir = new File("skins");
                            if (!skinDir.exists()) {
                                skinDir.mkdirs();
                            }
                            
                            // Create a unique file name for the skin
                            String fileName = "skin_" + uuid.toString() + ".png";
                            uploadedFile = new File(skinDir, fileName);
                            
                            // Write the file
                            try (FileOutputStream fos = new FileOutputStream(uploadedFile)) {
                                fos.write(item.get());
                            }
                            
                            // Set the skinType to custom
                            skinType = "custom";
                        }
                    }
                }
                
                // Apply the skin to the companion
                String skinPath = applySkin(companion, skinType, uploadedFile);
                
                // Return success response with skin path
                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.addProperty("message", "Skin uploaded successfully");
                response.addProperty("skinType", skinType);
                if (skinPath != null) {
                    response.addProperty("skinPath", skinPath);
                }
                
                resp.setContentType("application/json");
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(gson.toJson(response));
                
            } catch (FileUploadException e) {
                AICompanionMod.LOGGER.error("Error handling skin upload", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\": \"Error processing skin upload: " + e.getMessage() + "\"}");
            }
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Invalid companion ID format\"}");
        }
    }
    
    /**
     * Apply a skin to a companion entity
     * 
     * @param companion The AI companion entity
     * @param skinType The skin type to apply
     * @param customSkinFile The custom skin file (if any)
     * @return The skin path for custom skins, or null for built-in skins
     */
    private String applySkin(AICompanionEntity companion, String skinType, File customSkinFile) {
        String skinPath = null;
        
        if (customSkinFile != null) {
            // Get the relative path for the skin
            skinPath = "skins/" + customSkinFile.getName();
            AICompanionMod.LOGGER.info("Setting custom skin for companion " + companion.getUUID() + 
                " from file " + skinPath);
            
            // Set the custom skin path on the entity
            companion.setSkinPath(skinPath);
        } else {
            AICompanionMod.LOGGER.info("Setting skin type '" + skinType + "' for companion " + companion.getUUID());
            
            // Set the skin type on the entity
            companion.setSkinType(skinType);
            
            // Reset skin path for built-in skins
            companion.setSkinPath("");
        }
        
        return skinPath;
    }

    /**
     * Find a companion entity by its UUID
     */
    private AICompanionEntity findCompanionById(UUID uuid) {
        MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        if (server == null) {
            return null;
        }
        
        for (ServerWorld world : server.getAllLevels()) {
            AICompanionEntity companion = (AICompanionEntity) world.getEntity(uuid);
            if (companion != null) {
                return companion;
            }
        }
        
        return null;
    }
}