package br.com.healthcheck.infrastructure.util;

import java.util.*;

/**
 * Parser JSON simples usando apenas bibliotecas padrão do Java
 */
public class JsonParser {
    
    public static class JsonObject {
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
            int valueStartDepth = -1;
            
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
                    valueStartDepth = depth;
                    continue;
                } else if (!inKey && !inString && depth == valueStartDepth && c == ',' && i < json.length() - 1) {
                    if (key.length() > 0) {
                        String k = key.toString().trim().replaceAll("^[\"']+|[\"']+$", "");
                        String v = value.toString().trim();
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
            
            if (key.length() > 0) {
                String k = key.toString().trim().replaceAll("^[\"']+|[\"']+$", "");
                String v = value.toString().trim();
                obj.put(k, parseValue(v));
            }
        }
        
        private static Object parseValue(String value) {
            value = value.trim();
            if (value.isEmpty()) {
                return value;
            }
            
            if (value.startsWith("{") && value.endsWith("}")) {
                return parse(value);
            } else if (value.startsWith("[") && value.endsWith("]")) {
                return JsonArray.parse(value);
            } else if ((value.startsWith("\"") && value.endsWith("\"")) || 
                     (value.startsWith("'") && value.endsWith("'"))) {
                String unquoted = value.substring(1, value.length() - 1);
                return unquoted.replace("\\\"", "\"").replace("\\'", "'");
            } else if ("true".equals(value)) {
                return true;
            } else if ("false".equals(value)) {
                return false;
            } else if ("null".equals(value)) {
                return null;
            } else {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    try {
                        return Double.parseDouble(value);
                    } catch (NumberFormatException e2) {
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
        
        public Set<String> keySet() {
            return map.keySet();
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
        
        private String escapeJson(String str) {
            if (str == null) return "";
            return str.replace("\\", "\\\\")
                      .replace("\"", "\\\"")
                      .replace("\n", "\\n")
                      .replace("\r", "\\r")
                      .replace("\t", "\\t");
        }
    }
    
    public static class JsonArray {
        private List<Object> list = new ArrayList<>();
        
        public static JsonArray parse(String json) {
            JsonArray arr = new JsonArray();
            json = json.trim();
            if (!json.startsWith("[") || !json.endsWith("]")) {
                throw new IllegalArgumentException("Invalid JSON array");
            }
            json = json.substring(1, json.length() - 1).trim();
            if (json.isEmpty()) return arr;
            
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
        
        public Object get(int index) {
            return list.get(index);
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
        
        private String escapeJson(String str) {
            if (str == null) return "";
            return str.replace("\\", "\\\\")
                      .replace("\"", "\\\"")
                      .replace("\n", "\\n")
                      .replace("\r", "\\r")
                      .replace("\t", "\\t");
        }
    }
}

