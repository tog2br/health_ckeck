package br.com.healthcheck.domain.entity;

/**
 * Entidade de domínio representando um serviço a ser monitorado
 */
public class Service {
    private String name;
    private String url;
    private String category;
    private int expectedStatus;
    
    public Service(String name, String url, String category, int expectedStatus) {
        this.name = name;
        this.url = url;
        this.category = category != null ? category : "Geral";
        this.expectedStatus = expectedStatus > 0 ? expectedStatus : 200;
    }
    
    public String getName() {
        return name;
    }
    
    public String getUrl() {
        return url;
    }
    
    public String getCategory() {
        return category;
    }
    
    public int getExpectedStatus() {
        return expectedStatus;
    }
}

