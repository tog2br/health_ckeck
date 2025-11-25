const API_BASE = 'http://localhost:3000/api';

let autoRefreshInterval = null;
let config = null;
let currentEnvironment = 'homolog';

// Elementos do DOM
const refreshBtn = document.getElementById('refreshBtn');
const autoRefreshCheckbox = document.getElementById('autoRefresh');
const environmentSelect = document.getElementById('environmentSelect');
const servicesContainer = document.getElementById('servicesContainer');
const lastUpdateTime = document.getElementById('lastUpdateTime');
const totalValue = document.getElementById('totalValue');
const healthyValue = document.getElementById('healthyValue');
const unhealthyValue = document.getElementById('unhealthyValue');
const errorValue = document.getElementById('errorValue');

// Função para formatar tempo de resposta
function formatResponseTime(ms) {
    if (ms < 1000) {
        return `${ms}ms`;
    }
    return `${(ms / 1000).toFixed(2)}s`;
}

// Função para classificar tempo de resposta
function getResponseTimeClass(ms) {
    if (ms < 500) return 'fast';
    if (ms < 2000) return 'medium';
    return 'slow';
}

// Função para formatar data
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
}

// Função para criar card de serviço
function createServiceCard(service) {
    const statusClass = service.status;
    const responseTimeClass = getResponseTimeClass(service.responseTime);
    
    return `
        <div class="service-card ${statusClass}">
            <div class="service-header">
                <div class="service-name">${service.name}</div>
                <span class="service-status ${statusClass}">
                    ${service.status === 'healthy' ? '✓' : service.status === 'unhealthy' ? '⚠' : '✗'}
                    ${service.status === 'healthy' ? 'Operacional' : service.status === 'unhealthy' ? 'Problema' : 'Erro'}
                </span>
            </div>
            <div class="service-url">${service.url}</div>
            <div class="service-details">
                <div class="response-time ${responseTimeClass}">
                    ⏱ ${formatResponseTime(service.responseTime)}
                </div>
                <div class="status-code ${statusClass}">
                    Status: ${service.statusCode || 'N/A'}
                </div>
            </div>
            ${service.message ? `<div style="margin-top: 8px; font-size: 0.8rem; color: var(--text-secondary);">${service.message}</div>` : ''}
        </div>
    `;
}

// Função para renderizar serviços
function renderServices(data) {
    console.log('DEBUG: renderServices - data:', data);
    console.log('DEBUG: renderServices - data.services:', data.services);
    console.log('DEBUG: renderServices - Object.keys(data.services):', data.services ? Object.keys(data.services) : 'null');
    
    if (!data.services || Object.keys(data.services).length === 0) {
        console.log('DEBUG: Nenhum serviço encontrado');
        servicesContainer.innerHTML = '<div class="loading">Nenhum serviço configurado</div>';
        return;
    }

    let html = '';
    
    // Ordenar categorias
    const categories = Object.keys(data.services).sort();
    
    categories.forEach(category => {
        const services = data.services[category];
        const healthyCount = services.filter(s => s.status === 'healthy').length;
        const totalCount = services.length;
        
        html += `
            <div class="category-section">
                <div class="category-title">
                    ${category}
                    <span style="font-size: 0.9rem; color: var(--text-secondary); font-weight: normal;">
                        (${healthyCount}/${totalCount} operacionais)
                    </span>
                </div>
                <div class="services-grid">
                    ${services.map(service => createServiceCard(service)).join('')}
                </div>
            </div>
        `;
    });
    
    servicesContainer.innerHTML = html;
}

// Função para atualizar resumo
function updateSummary(summary) {
    totalValue.textContent = summary.total || 0;
    healthyValue.textContent = summary.healthy || 0;
    unhealthyValue.textContent = summary.unhealthy || 0;
    errorValue.textContent = summary.errors || 0;
    
    // Adicionar animação de pulso se houver mudanças
    if (summary.errors > 0 || summary.unhealthy > 0) {
        document.querySelector('.summary-card.error').classList.add('pulse');
        setTimeout(() => {
            document.querySelector('.summary-card.error').classList.remove('pulse');
        }, 2000);
    }
}

// Função para buscar status dos serviços
async function fetchHealthStatus() {
    try {
        refreshBtn.classList.add('loading');
        refreshBtn.disabled = true;
        
        const response = await fetch(`${API_BASE}/health`);
        const data = await response.json();
        
        console.log('DEBUG: Dados recebidos da API:', data);
        console.log('DEBUG: Services:', data.services);
        console.log('DEBUG: Summary:', data.summary);
        
        renderServices(data);
        updateSummary(data.summary);
        lastUpdateTime.textContent = formatDate(data.summary.timestamp);
        
    } catch (error) {
        console.error('Erro ao buscar status:', error);
        servicesContainer.innerHTML = `
            <div class="loading" style="color: var(--error-color);">
                Erro ao conectar com o servidor. Verifique se o servidor está rodando.
            </div>
        `;
    } finally {
        refreshBtn.classList.remove('loading');
        refreshBtn.disabled = false;
    }
}

// Função para configurar auto-refresh
function setupAutoRefresh() {
    if (autoRefreshInterval) {
        clearInterval(autoRefreshInterval);
        autoRefreshInterval = null;
    }
    
    if (autoRefreshCheckbox.checked && config) {
        const interval = config.refreshInterval || 30000;
        autoRefreshInterval = setInterval(fetchHealthStatus, interval);
    }
}

// Função para trocar ambiente
async function changeEnvironment(newEnv) {
    try {
        const response = await fetch(`${API_BASE}/environment`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ environment: newEnv })
        });
        
        const data = await response.json();
        if (data.success) {
            currentEnvironment = newEnv;
            environmentSelect.value = newEnv;
            // Recarregar configuração e dados
            await loadConfig();
            fetchHealthStatus();
        } else {
            console.error('Erro ao trocar ambiente:', data.error);
            alert('Erro ao trocar ambiente: ' + (data.error || 'Erro desconhecido'));
            // Reverter select
            environmentSelect.value = currentEnvironment;
        }
    } catch (error) {
        console.error('Erro ao trocar ambiente:', error);
        alert('Erro ao conectar com o servidor para trocar ambiente');
        // Reverter select
        environmentSelect.value = currentEnvironment;
    }
}

// Função para carregar ambiente atual
async function loadEnvironment() {
    try {
        const response = await fetch(`${API_BASE}/environment`);
        const data = await response.json();
        currentEnvironment = data.current || 'homolog';
        environmentSelect.value = currentEnvironment;
    } catch (error) {
        console.error('Erro ao carregar ambiente:', error);
    }
}

// Função para carregar configuração
async function loadConfig() {
    try {
        const response = await fetch(`${API_BASE}/config`);
        config = await response.json();
        // Atualizar ambiente se vier na resposta
        if (config.environment) {
            currentEnvironment = config.environment;
            environmentSelect.value = currentEnvironment;
        }
        setupAutoRefresh();
    } catch (error) {
        console.error('Erro ao carregar configuração:', error);
    }
}

// Event listeners
refreshBtn.addEventListener('click', () => {
    fetchHealthStatus();
});

autoRefreshCheckbox.addEventListener('change', () => {
    setupAutoRefresh();
});

environmentSelect.addEventListener('change', (e) => {
    const newEnv = e.target.value;
    if (newEnv !== currentEnvironment) {
        changeEnvironment(newEnv);
    }
});

// Carregar configuração e iniciar
async function init() {
    try {
        await loadEnvironment();
        await loadConfig();
        fetchHealthStatus();
    } catch (error) {
        console.error('Erro ao inicializar:', error);
        fetchHealthStatus(); // Tentar mesmo assim
    }
}

// Inicializar quando a página carregar
document.addEventListener('DOMContentLoaded', init);

// Atualizar quando a página voltar a ter foco
document.addEventListener('visibilitychange', () => {
    if (!document.hidden && autoRefreshCheckbox.checked) {
        fetchHealthStatus();
    }
});

