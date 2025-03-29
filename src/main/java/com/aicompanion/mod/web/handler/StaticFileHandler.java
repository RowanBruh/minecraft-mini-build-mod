package com.aicompanion.mod.web.handler;

import com.aicompanion.mod.AICompanionMod;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Handles serving static files for the web interface
 */
public class StaticFileHandler extends HttpServlet {
    private static final String WEB_ROOT = "/assets/aicompanion/web/";
    private static final int BUFFER_SIZE = 8192;
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Get requested path
        String path = req.getPathInfo();
        if (path == null || path.isEmpty() || path.equals("/")) {
            path = "/index.html";
        }
        
        // Normalize path
        path = path.replace("..", "").replace("//", "/");
        
        // Construct resource path
        String resourcePath = WEB_ROOT + path.substring(1);
        
        // Try to get the resource
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                // Resource not found, try to serve index.html for client-side routing
                if (!path.equals("/index.html")) {
                    try (InputStream indexIs = getClass().getResourceAsStream(WEB_ROOT + "index.html")) {
                        if (indexIs != null) {
                            setContentType(resp, "/index.html");
                            serveResource(indexIs, resp);
                            return;
                        }
                    }
                }
                
                // If we get here, the resource was not found
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("File not found: " + path);
                return;
            }
            
            // Set content type based on file extension
            setContentType(resp, path);
            
            // Serve the resource
            serveResource(is, resp);
        } catch (IOException e) {
            AICompanionMod.LOGGER.error("Error serving static file: " + path, e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error serving file: " + e.getMessage());
        }
    }
    
    /**
     * Set the content type based on file extension
     */
    private void setContentType(HttpServletResponse resp, String path) {
        String contentType = "application/octet-stream";
        
        if (path.endsWith(".html")) {
            contentType = "text/html";
        } else if (path.endsWith(".css")) {
            contentType = "text/css";
        } else if (path.endsWith(".js")) {
            contentType = "application/javascript";
        } else if (path.endsWith(".json")) {
            contentType = "application/json";
        } else if (path.endsWith(".png")) {
            contentType = "image/png";
        } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (path.endsWith(".gif")) {
            contentType = "image/gif";
        } else if (path.endsWith(".svg")) {
            contentType = "image/svg+xml";
        } else if (path.endsWith(".ico")) {
            contentType = "image/x-icon";
        }
        
        resp.setContentType(contentType);
    }
    
    /**
     * Serve a resource through the HttpServletResponse
     */
    private void serveResource(InputStream is, HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        
        try (OutputStream os = resp.getOutputStream()) {
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        }
    }
}