package br.com.healthcheck.presentation.handler;

import br.com.healthcheck.domain.usecase.CheckHealthUseCase;
import br.com.healthcheck.presentation.dto.HealthCheckResponse;
import br.com.healthcheck.infrastructure.util.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Handler HTTP para o endpoint /api/health
 */
public class HealthHandler implements HttpHandler {
    private final CheckHealthUseCase checkHealthUseCase;
    
    public HealthHandler(CheckHealthUseCase checkHealthUseCase) {
        this.checkHealthUseCase = checkHealthUseCase;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            return;
        }
        
        try {
            CheckHealthUseCase.HealthCheckSummary summary = checkHealthUseCase.execute();
            HealthCheckResponse response = new HealthCheckResponse(summary.getResults());
            
            JsonParser.JsonObject jsonResponse = toJson(response);
            sendResponse(exchange, 200, "application/json; charset=utf-8", jsonResponse.toJsonString());
            
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "application/json", 
                "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }
    
    private JsonParser.JsonObject toJson(HealthCheckResponse response) {
        JsonParser.JsonObject json = new JsonParser.JsonObject();
        
        // Services agrupados por categoria
        JsonParser.JsonObject servicesJson = new JsonParser.JsonObject();
        for (Map.Entry<String, List<HealthCheckResponse.ServiceHealthDto>> entry : 
             response.getServices().entrySet()) {
            JsonParser.JsonArray categoryArray = new JsonParser.JsonArray();
            for (HealthCheckResponse.ServiceHealthDto service : entry.getValue()) {
                categoryArray.add(toServiceJson(service));
            }
            servicesJson.put(entry.getKey(), categoryArray);
        }
        json.put("services", servicesJson);
        
        // Summary
        HealthCheckResponse.SummaryDto summary = response.getSummary();
        JsonParser.JsonObject summaryJson = new JsonParser.JsonObject();
        summaryJson.put("total", summary.getTotal());
        summaryJson.put("healthy", summary.getHealthy());
        summaryJson.put("unhealthy", summary.getUnhealthy());
        summaryJson.put("errors", summary.getErrors());
        summaryJson.put("timestamp", summary.getTimestamp());
        json.put("summary", summaryJson);
        
        return json;
    }
    
    private JsonParser.JsonObject toServiceJson(HealthCheckResponse.ServiceHealthDto service) {
        JsonParser.JsonObject json = new JsonParser.JsonObject();
        json.put("name", service.getName());
        json.put("url", service.getUrl());
        json.put("category", service.getCategory());
        json.put("status", service.getStatus());
        json.put("statusCode", service.getStatusCode());
        json.put("responseTime", service.getResponseTime());
        json.put("timestamp", service.getTimestamp());
        json.put("message", service.getMessage());
        
        if (service.getHealthDetails() != null) {
            JsonParser.JsonObject healthDetails = new JsonParser.JsonObject();
            healthDetails.put("rootStatus", service.getHealthDetails().getRootStatus());
            
            if (service.getHealthDetails().getComponents() != null) {
                JsonParser.JsonArray components = new JsonParser.JsonArray();
                for (HealthCheckResponse.ComponentDto comp : service.getHealthDetails().getComponents()) {
                    JsonParser.JsonObject compJson = new JsonParser.JsonObject();
                    compJson.put("name", comp.getName());
                    compJson.put("status", comp.getStatus());
                    components.add(compJson);
                }
                healthDetails.put("components", components);
            }
            
            json.put("healthDetails", healthDetails);
        }
        
        return json;
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

