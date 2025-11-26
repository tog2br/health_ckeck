package br.com.healthcheck.presentation.handler;

import br.com.healthcheck.infrastructure.config.EnvironmentManager;
import br.com.healthcheck.infrastructure.util.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Handler HTTP para o endpoint /api/environment
 */
public class EnvironmentHandler implements HttpHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equals("GET")) {
            handleGet(exchange);
        } else if (exchange.getRequestMethod().equals("POST")) {
            handlePost(exchange);
        } else {
            sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
        }
    }
    
    private void handleGet(HttpExchange exchange) throws IOException {
        try {
            JsonParser.JsonObject response = new JsonParser.JsonObject();
            response.put("current", EnvironmentManager.getEnvironment());
            
            JsonParser.JsonArray available = new JsonParser.JsonArray();
            available.add("homolog");
            available.add("prod");
            response.put("available", available);
            
            sendResponse(exchange, 200, "application/json; charset=utf-8", response.toJsonString());
            
        } catch (Exception e) {
            sendResponse(exchange, 500, "application/json", 
                "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }
    
    private void handlePost(HttpExchange exchange) throws IOException {
        try {
            String requestBody = readRequestBody(exchange);
            JsonParser.JsonObject request = JsonParser.JsonObject.parse(requestBody);
            
            Object envObj = request.get("environment");
            String env = null;
            
            if (envObj != null) {
                env = envObj.toString();
                // Remover aspas se houver
                env = env.trim();
                while ((env.startsWith("\"") || env.startsWith("'")) && 
                       (env.endsWith("\"") || env.endsWith("'"))) {
                    env = env.substring(1, env.length() - 1).trim();
                }
                env = env.replaceAll("^[\"']+|[\"']+$", "");
            }
            
            if (env == null || env.isEmpty()) {
                sendResponse(exchange, 400, "application/json", 
                    "{\"error\":\"Campo 'environment' não encontrado ou vazio\"}");
                return;
            }
            
            env = env.trim().toLowerCase();
            
            if ("prod".equals(env) || "homolog".equals(env)) {
                EnvironmentManager.setEnvironment(env);
                JsonParser.JsonObject response = new JsonParser.JsonObject();
                response.put("success", true);
                response.put("environment", EnvironmentManager.getEnvironment());
                response.put("message", "Ambiente alterado para: " + EnvironmentManager.getEnvironment());
                sendResponse(exchange, 200, "application/json; charset=utf-8", response.toJsonString());
            } else {
                sendResponse(exchange, 400, "application/json", 
                    "{\"error\":\"Ambiente inválido: '" + escapeJson(env) + "'. Use 'homolog' ou 'prod'\"}");
            }
            
        } catch (Exception e) {
            sendResponse(exchange, 500, "application/json", 
                "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }
    
    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(line);
            }
            return sb.toString();
        }
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, 
                             String contentType, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (java.io.OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
        exchange.close();
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}

