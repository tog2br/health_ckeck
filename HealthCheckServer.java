import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;

/**
 * Servidor Health Check - Usa APENAS bibliotecas padrão do Java
 * Não requer nenhuma dependência externa
 * Requer Java 8+
 */
public class HealthCheckServer {
    private static final int PORT = 3000;
    private static final String CONFIG_HOMOLOG = "config-homolog.json";
    private static final String CONFIG_PROD = "config-prod.json";
    private static final String PUBLIC_DIR = "public";
    
    // Ambiente atual (padrão: homolog)
    private static volatile String currentEnvironment = "homolog";
    
    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            
            // Rota para a página principal
            server.createContext("/", new StaticFileHandler());
            server.createContext("/index.html", new StaticFileHandler());
            
            // API endpoints
            server.createContext("/api/health", new HealthHandler());
            server.createContext("/api/config", new ConfigHandler());
            server.createContext("/api/environment", new EnvironmentHandler());
            
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            
            System.out.println("🚀 Servidor Health Check rodando em http://localhost:" + PORT);
            System.out.println("📊 Dashboard disponível em http://localhost:" + PORT);
            System.out.println("🌍 Ambiente inicial: " + currentEnvironment);
            System.out.println("📝 Pressione Ctrl+C para parar o servidor");
            
        } catch (IOException e) {
            System.err.println("❌ Erro ao iniciar servidor: " + e.getMessage());
            if (e.getMessage().contains("Address already in use")) {
                System.err.println("   A porta " + PORT + " já está em uso.");
            }
            System.exit(1);
        }
    }
    
    private static String getConfigFile() {
        if ("prod".equals(currentEnvironment)) {
            return CONFIG_PROD;
        }
        return CONFIG_HOMOLOG;
    }
    
    private static synchronized void setEnvironment(String env) {
        if ("prod".equals(env) || "homolog".equals(env)) {
            currentEnvironment = env;
        }
    }
    
    private static synchronized String getEnvironment() {
        return currentEnvironment;
    }
    
    // Handler para arquivos estáticos
    static class StaticFileHandler implements HttpHandler {
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
    }
    
    // Handler para /api/health
    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equals("GET")) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }
            
            try {
                JsonObject config = loadConfig();
                JsonArray services = config.getArray("services");
                int timeout = config.getInt("timeout", 5000);
                
                System.out.println("DEBUG: Health check - Ambiente: " + getEnvironment());
                System.out.println("DEBUG: Health check - Número de serviços: " + services.size());
                
                List<Future<JsonObject>> futures = new ArrayList<>();
                ExecutorService executor = Executors.newCachedThreadPool();
                
                // Verificar todos os serviços em paralelo
                for (int i = 0; i < services.size(); i++) {
                    JsonObject service = services.getObject(i);
                    futures.add(executor.submit(() -> checkHealth(service, timeout)));
                }
                
                // Coletar resultados
                JsonObject grouped = new JsonObject();
                int total = 0, healthy = 0, unhealthy = 0, errors = 0;
                
                for (Future<JsonObject> future : futures) {
                    try {
                        JsonObject result = future.get();
                        String category = result.getString("category");
                        
                        if (!grouped.hasKey(category)) {
                            grouped.put(category, new JsonArray());
                        }
                        grouped.getArray(category).add(result);
                        
                        total++;
                        String status = result.getString("status");
                        if ("healthy".equals(status)) healthy++;
                        else if ("unhealthy".equals(status)) unhealthy++;
                        else errors++;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
                executor.shutdown();
                
                JsonObject summary = new JsonObject();
                summary.put("total", total);
                summary.put("healthy", healthy);
                summary.put("unhealthy", unhealthy);
                summary.put("errors", errors);
                summary.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                
                JsonObject response = new JsonObject();
                response.put("services", grouped);
                response.put("summary", summary);
                
                sendResponse(exchange, 200, "application/json; charset=utf-8", response.toJsonString());
                
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "application/json", 
                    "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
        
        private JsonObject checkHealth(JsonObject service, int timeout) {
            long startTime = System.currentTimeMillis();
            String url = service.getString("url");
            String name = service.getString("name");
            String category = service.getString("category", "Geral");
            int expectedStatus = service.getInt("expectedStatus", 200);
            
            JsonObject result = new JsonObject();
            result.put("name", name);
            result.put("url", url);
            result.put("category", category);
            
            try {
                URL urlObj = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(timeout);
                conn.setReadTimeout(timeout);
                conn.setRequestProperty("User-Agent", "HealthCheck/1.0");
                
                int statusCode = conn.getResponseCode();
                long responseTime = System.currentTimeMillis() - startTime;
                
                boolean isHealthy = statusCode == expectedStatus;
                
                result.put("status", isHealthy ? "healthy" : "unhealthy");
                result.put("statusCode", statusCode);
                result.put("responseTime", (int)responseTime);
                result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                result.put("message", isHealthy ? "Operacional" : "Status " + statusCode);
                
            } catch (SocketTimeoutException e) {
                long responseTime = System.currentTimeMillis() - startTime;
                result.put("status", "error");
                result.put("statusCode", 0);
                result.put("responseTime", (int)responseTime);
                result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                result.put("message", "Timeout");
                
            } catch (Exception e) {
                long responseTime = System.currentTimeMillis() - startTime;
                result.put("status", "error");
                result.put("statusCode", 0);
                result.put("responseTime", (int)responseTime);
                result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                String msg = e.getMessage();
                result.put("message", msg != null ? msg : "Erro de conexão");
            }
            
            return result;
        }
    }
    
    // Handler para /api/config
    static class ConfigHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equals("GET")) {
                try {
                    JsonObject config = loadConfig();
                    // Adicionar informação do ambiente atual
                    config.put("environment", getEnvironment());
                    sendResponse(exchange, 200, "application/json; charset=utf-8", config.toJsonString());
                } catch (Exception e) {
                    sendResponse(exchange, 500, "application/json", 
                        "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
                }
            } else if (exchange.getRequestMethod().equals("POST")) {
                try {
                    String requestBody = readRequestBody(exchange);
                    JsonObject config = JsonObject.parse(requestBody);
                    
                    // Remover campo environment se existir (não salvar)
                    String configFile = getConfigFile();
                    
                    // Salvar configuração no arquivo do ambiente atual
                    try (FileWriter writer = new FileWriter(configFile, StandardCharsets.UTF_8)) {
                        writer.write(config.toJsonString());
                    }
                    
                    JsonObject response = new JsonObject();
                    response.put("success", true);
                    response.put("message", "Configuração atualizada com sucesso");
                    response.put("environment", getEnvironment());
                    sendResponse(exchange, 200, "application/json; charset=utf-8", response.toJsonString());
                    
                } catch (Exception e) {
                    sendResponse(exchange, 500, "application/json", 
                        "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
                }
            } else {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            }
        }
    }
    
    // Handler para /api/environment
    static class EnvironmentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equals("GET")) {
                try {
                    JsonObject response = new JsonObject();
                    response.put("current", getEnvironment());
                    JsonArray available = new JsonArray();
                    available.add("homolog");
                    available.add("prod");
                    response.put("available", available);
                    sendResponse(exchange, 200, "application/json; charset=utf-8", response.toJsonString());
                } catch (Exception e) {
                    sendResponse(exchange, 500, "application/json", 
                        "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
                }
            } else if (exchange.getRequestMethod().equals("POST")) {
                try {
                    String requestBody = readRequestBody(exchange);
                    System.out.println("DEBUG: Request body recebido: " + requestBody);
                    
                    JsonObject request = JsonObject.parse(requestBody);
                    Object envObj = request.get("environment");
                    String env = null;
                    
                    if (envObj != null) {
                        env = envObj.toString();
                        // Remover aspas simples ou duplas (no início e fim) - pode ter múltiplas camadas
                        env = env.trim();
                        while ((env.startsWith("\"") || env.startsWith("'")) && 
                               (env.endsWith("\"") || env.endsWith("'"))) {
                            env = env.substring(1, env.length() - 1).trim();
                        }
                        // Remover qualquer aspa que tenha sobrado no início ou fim
                        env = env.replaceAll("^[\"']+|[\"']+$", "");
                        System.out.println("DEBUG: Environment extraído: '" + env + "'");
                    } else {
                        System.out.println("DEBUG: Campo 'environment' não encontrado no JSON");
                        sendResponse(exchange, 400, "application/json", 
                            "{\"error\":\"Campo 'environment' não encontrado no JSON\"}");
                        return;
                    }
                    
                    if (env == null || env.isEmpty()) {
                        sendResponse(exchange, 400, "application/json", 
                            "{\"error\":\"Campo 'environment' está vazio\"}");
                        return;
                    }
                    
                    // Normalizar: remover espaços e converter para lowercase
                    env = env.trim().toLowerCase();
                    System.out.println("DEBUG: Environment normalizado: '" + env + "'");
                    
                    if ("prod".equals(env) || "homolog".equals(env)) {
                        setEnvironment(env);
                        JsonObject response = new JsonObject();
                        response.put("success", true);
                        response.put("environment", getEnvironment());
                        response.put("message", "Ambiente alterado para: " + getEnvironment());
                        sendResponse(exchange, 200, "application/json; charset=utf-8", response.toJsonString());
                    } else {
                        sendResponse(exchange, 400, "application/json", 
                            "{\"error\":\"Ambiente inválido: '" + escapeJson(env) + "'. Use 'homolog' ou 'prod'\"}");
                    }
                } catch (Exception e) {
                    e.printStackTrace(); // Debug
                    sendResponse(exchange, 500, "application/json", 
                        "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
                }
            } else {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            }
        }
    }
    
    // Utilitários
    private static JsonObject loadConfig() throws IOException {
        try {
            String configFile = getConfigFile();
            System.out.println("DEBUG: Carregando arquivo de configuração: " + configFile);
            System.out.println("DEBUG: Ambiente atual: " + getEnvironment());
            
            if (!Files.exists(Paths.get(configFile))) {
                System.out.println("DEBUG: Arquivo não encontrado: " + configFile);
                JsonObject defaultConfig = new JsonObject();
                defaultConfig.put("refreshInterval", 30000);
                defaultConfig.put("timeout", 5000);
                defaultConfig.put("services", new JsonArray());
                return defaultConfig;
            }
            
            String content = new String(Files.readAllBytes(Paths.get(configFile)), StandardCharsets.UTF_8);
            System.out.println("DEBUG: Conteúdo do arquivo (primeiros 500 chars): " + content.substring(0, Math.min(500, content.length())));
            
            JsonObject config = JsonObject.parse(content);
            
            // Verificar se tem serviços
            Object servicesObj = config.get("services");
            System.out.println("DEBUG: servicesObj tipo: " + (servicesObj != null ? servicesObj.getClass().getName() : "null"));
            
            JsonArray services = config.getArray("services");
            System.out.println("DEBUG: Número de serviços carregados: " + services.size());
            
            if (services.size() > 0) {
                System.out.println("DEBUG: Primeiro serviço: " + services.getObject(0));
            }
            
            return config;
        } catch (FileNotFoundException e) {
            System.out.println("DEBUG: FileNotFoundException: " + e.getMessage());
            // Retornar configuração padrão
            JsonObject defaultConfig = new JsonObject();
            defaultConfig.put("refreshInterval", 30000);
            defaultConfig.put("timeout", 5000);
            defaultConfig.put("services", new JsonArray());
            return defaultConfig;
        } catch (Exception e) {
            System.out.println("DEBUG: Erro ao carregar config: " + e.getMessage());
            e.printStackTrace();
            // Retornar configuração padrão
            JsonObject defaultConfig = new JsonObject();
            defaultConfig.put("refreshInterval", 30000);
            defaultConfig.put("timeout", 5000);
            defaultConfig.put("services", new JsonArray());
            return defaultConfig;
        }
    }
    
    private static String readRequestBody(HttpExchange exchange) throws IOException {
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
    
    private static void sendResponse(HttpExchange exchange, int statusCode, 
                                   String contentType, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
        exchange.close();
    }
    
    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    // Classes simples para manipular JSON (sem dependências externas)
    static class JsonObject {
        private Map<String, Object> map = new LinkedHashMap<>();
        
        public static JsonObject parse(String json) {
            JsonObject obj = new JsonObject();
            json = json.trim();
            if (!json.startsWith("{") || !json.endsWith("}")) {
                throw new IllegalArgumentException("Invalid JSON object");
            }
            json = json.substring(1, json.length() - 1).trim();
            if (json.isEmpty()) return obj;
            
            parseObject(json, obj);
            return obj;
        }
        
        private static void parseObject(String json, JsonObject obj) {
            int depth = 0;
            StringBuilder key = new StringBuilder();
            StringBuilder value = new StringBuilder();
            boolean inKey = true;
            boolean inString = false;
            char stringChar = 0;
            int valueStartDepth = -1; // Profundidade quando começamos a ler o valor (-1 = não começou)
            
            for (int i = 0; i < json.length(); i++) {
                char c = json.charAt(i);
                
                if (!inString && (c == '{' || c == '[')) depth++;
                else if (!inString && (c == '}' || c == ']')) depth--;
                else if (!inString && c == '"' && (i == 0 || json.charAt(i-1) != '\\')) {
                    inString = true;
                    stringChar = '"';
                } else if (inString && c == stringChar && json.charAt(i-1) != '\\') {
                    inString = false;
                }
                
                if (!inString && depth == 0 && c == ':') {
                    inKey = false;
                    valueStartDepth = depth; // Marcar profundidade inicial do valor (0)
                    continue; // Não adicionar ':' nem ao key nem ao value
                } else if (!inKey && !inString && depth == valueStartDepth && c == ',' && i < json.length() - 1) {
                    // Só parar se estivermos na mesma profundidade que começamos o valor (0)
                    // e não for o último caractere
                    if (key.length() > 0) {
                        String k = key.toString().trim().replaceAll("^[\"']+|[\"']+$", "");
                        String v = value.toString().trim();
                        System.out.println("DEBUG: parseObject - key: '" + k + "', value length: " + v.length() + ", ends with: '" + (v.length() > 0 ? v.substring(Math.max(0, v.length() - 20)) : "") + "'");
                        obj.put(k, parseValue(v));
                        key.setLength(0);
                        value.setLength(0);
                        inKey = true;
                        valueStartDepth = -1;
                    }
                } else if (inKey) {
                    key.append(c);
                } else if (!inKey) {
                    value.append(c);
                }
            }
            
            // Processar último par key-value se houver
            if (key.length() > 0) {
                String k = key.toString().trim().replaceAll("^[\"']+|[\"']+$", "");
                String v = value.toString().trim();
                System.out.println("DEBUG: parseObject (final) - key: '" + k + "', value length: " + v.length() + ", ends with: '" + (v.length() > 0 ? v.substring(Math.max(0, v.length() - 20)) : "") + "'");
                obj.put(k, parseValue(v));
            }
        }
        
        private static Object parseValue(String value) {
            value = value.trim();
            if (value.isEmpty()) {
                return value;
            }
            
            // Verificar se é um objeto JSON (deve começar com { e terminar com })
            if (value.startsWith("{") && value.endsWith("}")) {
                System.out.println("DEBUG: parseValue - detectado objeto JSON");
                return parse(value);
            } 
            // Verificar se é um array JSON (deve começar com [ e terminar com ])
            else if (value.startsWith("[") && value.endsWith("]")) {
                System.out.println("DEBUG: parseValue - detectado array JSON, tamanho: " + value.length());
                try {
                    JsonArray arr = JsonArray.parse(value);
                    System.out.println("DEBUG: parseValue - array parseado com " + arr.size() + " elementos");
                    return arr;
                } catch (Exception e) {
                    System.out.println("DEBUG: parseValue - erro ao parsear array: " + e.getMessage());
                    e.printStackTrace();
                    return value; // Retornar como string se falhar
                }
            } 
            // Verificar se é uma string (entre aspas)
            else if ((value.startsWith("\"") && value.endsWith("\"")) || 
                     (value.startsWith("'") && value.endsWith("'"))) {
                String unquoted = value.substring(1, value.length() - 1);
                return unquoted.replace("\\\"", "\"").replace("\\'", "'");
            } 
            // Verificar booleanos
            else if ("true".equals(value)) {
                return true;
            } else if ("false".equals(value)) {
                return false;
            } 
            // Verificar null
            else if ("null".equals(value)) {
                return null;
            } 
            // Tentar números
            else {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    try {
                        return Double.parseDouble(value);
                    } catch (NumberFormatException e2) {
                        System.out.println("DEBUG: parseValue - retornando como string: '" + value.substring(0, Math.min(50, value.length())) + "'");
                        return value;
                    }
                }
            }
        }
        
        public void put(String key, Object value) {
            map.put(key, value);
        }
        
        public Object get(String key) {
            return map.get(key);
        }
        
        public String getString(String key) {
            Object val = map.get(key);
            return val != null ? val.toString() : null;
        }
        
        public String getString(String key, String defaultValue) {
            Object val = map.get(key);
            return val != null ? val.toString() : defaultValue;
        }
        
        public int getInt(String key, int defaultValue) {
            Object val = map.get(key);
            if (val instanceof Number) {
                return ((Number) val).intValue();
            }
            try {
                return Integer.parseInt(val.toString());
            } catch (Exception e) {
                return defaultValue;
            }
        }
        
        public JsonArray getArray(String key) {
            Object val = map.get(key);
            return val instanceof JsonArray ? (JsonArray) val : new JsonArray();
        }
        
        public boolean hasKey(String key) {
            return map.containsKey(key);
        }
        
        public String toJsonString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":");
                sb.append(valueToJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        
        private String valueToJson(Object value) {
            if (value == null) return "null";
            if (value instanceof String) return "\"" + escapeJson(value.toString()) + "\"";
            if (value instanceof JsonObject) return ((JsonObject) value).toJsonString();
            if (value instanceof JsonArray) return ((JsonArray) value).toJsonString();
            if (value instanceof Boolean) return value.toString();
            return value.toString();
        }
    }
    
    static class JsonArray {
        private List<Object> list = new ArrayList<>();
        
        public static JsonArray parse(String json) {
            JsonArray arr = new JsonArray();
            json = json.trim();
            if (!json.startsWith("[") || !json.endsWith("]")) {
                throw new IllegalArgumentException("Invalid JSON array");
            }
            json = json.substring(1, json.length() - 1).trim();
            if (json.isEmpty()) return arr;
            
            // Parsing simples de array
            int depth = 0;
            boolean inString = false;
            StringBuilder current = new StringBuilder();
            
            for (int i = 0; i < json.length(); i++) {
                char c = json.charAt(i);
                
                if (!inString && (c == '{' || c == '[')) depth++;
                else if (!inString && (c == '}' || c == ']')) depth--;
                else if (!inString && c == '"' && (i == 0 || json.charAt(i-1) != '\\')) {
                    inString = true;
                } else if (inString && c == '"' && json.charAt(i-1) != '\\') {
                    inString = false;
                }
                
                if (!inString && depth == 0 && c == ',') {
                    if (current.length() > 0) {
                        arr.add(JsonObject.parseValue(current.toString().trim()));
                        current.setLength(0);
                    }
                } else {
                    current.append(c);
                }
            }
            
            if (current.length() > 0) {
                arr.add(JsonObject.parseValue(current.toString().trim()));
            }
            
            return arr;
        }
        
        public void add(Object value) {
            list.add(value);
        }
        
        public int size() {
            return list.size();
        }
        
        public JsonObject getObject(int index) {
            Object val = list.get(index);
            return val instanceof JsonObject ? (JsonObject) val : null;
        }
        
        public String toJsonString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                Object val = list.get(i);
                if (val instanceof JsonObject) {
                    sb.append(((JsonObject) val).toJsonString());
                } else if (val instanceof JsonArray) {
                    sb.append(((JsonArray) val).toJsonString());
                } else if (val instanceof String) {
                    sb.append("\"").append(escapeJson(val.toString())).append("\"");
                } else {
                    sb.append(val);
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }
}
