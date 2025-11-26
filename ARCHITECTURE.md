# 🏗️ Arquitetura do Projeto

Este projeto segue os princípios de **Clean Architecture**, separando as responsabilidades em camadas bem definidas.

## 📁 Estrutura de Diretórios

```
src/main/java/br/com/healthcheck/
├── domain/                      # Camada de Domínio (regras de negócio)
│   ├── entity/                 # Entidades de domínio
│   │   ├── Service.java        # Entidade representando um serviço
│   │   ├── HealthCheckResult.java  # Resultado de um health check
│   │   └── Component.java      # Componente de health check
│   ├── repository/             # Interfaces de repositório
│   │   ├── ConfigRepository.java
│   │   └── HealthCheckRepository.java
│   └── usecase/                # Casos de uso
│       ├── CheckHealthUseCase.java
│       ├── GetConfigUseCase.java
│       └── SaveConfigUseCase.java
│
├── data/                        # Camada de Dados
│   └── repository/             # Implementações de repositório
│       ├── JsonConfigRepository.java    # Implementação usando arquivo JSON
│       └── HttpHealthCheckRepository.java  # Implementação usando HTTP
│
├── presentation/               # Camada de Apresentação
│   ├── dto/                    # Data Transfer Objects
│   │   ├── HealthCheckResponse.java
│   │   └── ConfigResponse.java
│   └── handler/                 # Handlers HTTP
│       ├── HealthHandler.java
│       ├── ConfigHandler.java
│       ├── EnvironmentHandler.java
│       └── StaticFileHandler.java
│
└── infrastructure/             # Camada de Infraestrutura
    ├── config/                  # Configurações
    │   └── EnvironmentManager.java
    ├── server/                  # Servidor HTTP
    │   └── HealthCheckServer.java  # Classe principal
    └── util/                    # Utilitários
        └── JsonParser.java      # Parser JSON customizado
```

## 🔄 Fluxo de Dados

### 1. Requisição HTTP
```
Cliente → HealthHandler → CheckHealthUseCase
```

### 2. Caso de Uso
```
CheckHealthUseCase:
  - Busca serviços do ConfigRepository
  - Para cada serviço, chama HealthCheckRepository
  - Retorna HealthCheckSummary
```

### 3. Repositórios
```
ConfigRepository (interface) → JsonConfigRepository (implementação)
HealthCheckRepository (interface) → HttpHealthCheckRepository (implementação)
```

### 4. Resposta
```
HealthCheckSummary → HealthCheckResponse (DTO) → JSON → Cliente
```

## 🎯 Princípios Aplicados

### Dependency Inversion
- As camadas externas (Data, Presentation) dependem de interfaces definidas na camada Domain
- Facilita testes e troca de implementações

### Single Responsibility
- Cada classe tem uma única responsabilidade
- Use cases contêm apenas lógica de negócio
- Handlers apenas convertem HTTP para chamadas de use cases

### Separation of Concerns
- **Domain**: Regras de negócio puras, sem dependências externas
- **Data**: Acesso a dados (arquivos, HTTP)
- **Presentation**: Interface HTTP e DTOs
- **Infrastructure**: Configuração e utilitários

## 📦 Dependências entre Camadas

```
Infrastructure → Presentation → Domain
                Data → Domain
```

**Regra**: Dependências sempre apontam para dentro (Domain é o centro)

## 🧪 Testabilidade

Com essa arquitetura, é fácil:
- Mockar repositórios para testar use cases
- Testar lógica de negócio isoladamente
- Trocar implementações sem afetar outras camadas

## 🔧 Como Adicionar Novas Funcionalidades

1. **Nova Entidade**: Criar em `domain/entity/`
2. **Novo Caso de Uso**: Criar em `domain/usecase/`
3. **Nova Interface**: Criar em `domain/repository/`
4. **Nova Implementação**: Criar em `data/repository/`
5. **Novo Handler**: Criar em `presentation/handler/`
6. **Registrar Handler**: Adicionar em `HealthCheckServer.java`

