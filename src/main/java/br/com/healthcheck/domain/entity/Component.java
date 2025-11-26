package br.com.healthcheck.domain.entity;

/**
 * Entidade de domínio representando um componente de health check
 */
public class Component {
    private String name;
    private String status; // UP, DOWN, UNKNOWN
    
    public Component(String name, String status) {
        this.name = name;
        this.status = status;
    }
    
    public String getName() {
        return name;
    }
    
    public String getStatus() {
        return status;
    }
}

