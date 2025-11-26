package br.com.healthcheck.presentation.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

/**
 * Handler HTTP para servir arquivos estáticos
 */
public class StaticFileHandler implements HttpHandler {
    private static final String PUBLIC_DIR = "public";
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        
        // Normalizar caminho
        if (path.equals("/") || path.equals("/index.html")) {
            path = PUBLIC_DIR + "/index.html";
        } else {
            // Remover barra inicial
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            
            // Se não começar com public/, adicionar
            if (!path.startsWith(PUBLIC_DIR + "/")) {
                path = PUBLIC_DIR + "/" + path;
            }
        }
        
        File file = new File(path);
        
        if (!file.exists() || file.isDirectory()) {
            sendResponse(exchange, 404, "text/plain; charset=utf-8", "Arquivo não encontrado: " + path);
            return;
        }
        
        String contentType = getContentType(file.getName());
        byte[] fileContent = Files.readAllBytes(file.toPath());
        
        sendResponse(exchange, 200, contentType, new String(fileContent, StandardCharsets.UTF_8));
    }
    
    private String getContentType(String filename) {
        if (filename.endsWith(".html")) return "text/html; charset=utf-8";
        if (filename.endsWith(".css")) return "text/css; charset=utf-8";
        if (filename.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (filename.endsWith(".json")) return "application/json; charset=utf-8";
        return "application/octet-stream";
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, 
                             String contentType, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (java.io.OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
        exchange.close();
    }
}

