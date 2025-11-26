package br.com.healthcheck.infrastructure.config;

/**
 * Gerenciador de ambiente (homolog/prod)
 */
public class EnvironmentManager {
    private static volatile String currentEnvironment = "homolog";
    private static final String CONFIG_HOMOLOG = "config-homolog.json";
    private static final String CONFIG_PROD = "config-prod.json";
    
    public static synchronized void setEnvironment(String env) {
        if ("prod".equals(env) || "homolog".equals(env)) {
            currentEnvironment = env;
        }
    }
    
    public static synchronized String getEnvironment() {
        return currentEnvironment;
    }
    
    public static String getConfigFile() {
        if ("prod".equals(currentEnvironment)) {
            return CONFIG_PROD;
        }
        return CONFIG_HOMOLOG;
    }
}

