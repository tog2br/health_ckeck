# 🏥 Health Check Dashboard

Dashboard moderno e intuitivo para monitoramento de saúde de APIs e serviços.

## 🚀 Funcionalidades

- ✅ **Monitoramento em tempo real** - Verifica o status de múltiplos serviços
- 🌍 **Múltiplos ambientes** - Suporte para Homologação e Produção com troca instantânea
- 🔄 **Auto-refresh configurável** - Atualização automática dos status
- 📊 **Resumo visual** - Estatísticas gerais de todos os serviços
- 🎨 **Design minimalista** - Interface moderna e intuitiva
- 📁 **Agrupamento por categoria** - Organize seus serviços por tipo
- ⚡ **Tempo de resposta** - Visualize a performance de cada serviço
- 🎯 **Status colorido** - Verde (operacional), Amarelo (problema), Vermelho (erro)
- 📝 **Configuração por ambiente** - Arquivos separados para cada ambiente

## 📋 Pré-requisitos

- **Java 8+** (JDK - Java Development Kit)
- **Nenhuma dependência externa!** Usa apenas bibliotecas padrão do Java

## 🔧 Instalação

**Não é necessário instalar dependências!** O projeto usa apenas bibliotecas padrão do Java.

1. Clone ou baixe o projeto
2. Verifique se o Java está instalado:

```bash
javac -version
```

## 🎯 Como Usar

1. **Configure os serviços** editando os arquivos de configuração:
   - `config-homolog.json` - Configuração para ambiente de homologação
   - `config-prod.json` - Configuração para ambiente de produção

2. **Compile e execute**:

```bash
./compile.sh
```

O script irá:
- Compilar todos os arquivos Java da estrutura Clean Architecture
- Criar o diretório `target/classes` com os arquivos compilados
- Executar o servidor

**Estrutura de compilação:**
- Código fonte: `src/main/java/br/com/healthcheck/`
- Classes compiladas: `target/classes/`
- Classe principal: `br.com.healthcheck.infrastructure.server.HealthCheckServer`

3. **Acesse o dashboard**: `http://localhost:3000`

4. **Troque entre ambientes** usando o seletor no topo do dashboard

## 📝 Configuração

O projeto suporta **múltiplos ambientes** (Homologação e Produção). Cada ambiente tem seu próprio arquivo de configuração:

- **`config-homolog.json`** - Configuração para ambiente de homologação
- **`config-prod.json`** - Configuração para ambiente de produção

### Estrutura do arquivo de configuração:

```json
{
  "refreshInterval": 30000,
  "timeout": 5000,
  "services": [
    {
      "name": "Nome do Serviço",
      "url": "https://api.exemplo.com/health",
      "category": "Categoria",
      "expectedStatus": 200
    }
  ]
}
```

### Parâmetros

- **refreshInterval**: Intervalo de atualização em milissegundos (padrão: 30000 = 30s)
- **timeout**: Timeout para cada requisição em milissegundos (padrão: 5000 = 5s)
- **services**: Array de serviços para monitorar
  - **name**: Nome exibido no dashboard
  - **url**: URL do endpoint de health check
  - **category**: Categoria para agrupamento (ex: "Backend", "Frontend", "Infraestrutura")
  - **expectedStatus**: Status HTTP esperado (padrão: 200)

### Trocar entre Ambientes

No dashboard, use o seletor **"🌍 Ambiente"** no topo da página para alternar entre:
- **Homologação** - Carrega serviços de `config-homolog.json`
- **Produção** - Carrega serviços de `config-prod.json`

A troca de ambiente é instantânea e recarrega automaticamente os serviços do ambiente selecionado.

## ⚙️ Configuração

### Parâmetros do config.json

- **refreshInterval**: Intervalo de atualização automática em milissegundos (padrão: 30000 = 30 segundos)
- **timeout**: Timeout para cada requisição em milissegundos (padrão: 5000 = 5 segundos)
- **services**: Array de serviços para monitorar
  - **name**: Nome exibido no dashboard
  - **url**: URL do endpoint de health check
  - **category**: Categoria para agrupamento (ex: "Backend", "Frontend", "Infraestrutura")
  - **expectedStatus**: Status HTTP esperado (padrão: 200)

### Exemplo de configuração

```json
{
  "refreshInterval": 30000,
  "timeout": 5000,
  "services": [
    {
      "name": "API Principal",
      "url": "https://api.exemplo.com/health",
      "category": "Backend",
      "expectedStatus": 200
    },
    {
      "name": "API de Autenticação",
      "url": "https://auth.exemplo.com/health",
      "category": "Backend",
      "expectedStatus": 200
    },
    {
      "name": "Serviço de Notificações",
      "url": "https://notifications.exemplo.com/health",
      "category": "Serviços",
      "expectedStatus": 200
    }
  ]
}
```

## 🎨 Interface

O dashboard exibe:

- **Cards de resumo**: Total, Operacionais, Com Problemas, Erros
- **Serviços agrupados por categoria**: Organização visual clara
- **Status visual**: Cores indicam a saúde de cada serviço
- **Tempo de resposta**: Performance de cada endpoint
- **Última atualização**: Timestamp da última verificação

## 🔄 Atualização em Tempo Real

O dashboard atualiza automaticamente a cada intervalo configurado. Você pode:

- Ativar/desativar o auto-refresh usando o checkbox
- Atualizar manualmente clicando no botão "Atualizar"
- O dashboard também atualiza quando a aba volta a ter foco

## 🛠️ Tecnologias

- **Backend**: Java 8+ (bibliotecas padrão: com.sun.net.httpserver, java.net)
- **Frontend**: HTML5 + CSS3 + JavaScript (Vanilla)
- **Arquitetura**: Clean Architecture com separação de responsabilidades
- **Sem dependências externas**: Funciona apenas com JDK padrão!
- **Suporte a múltiplos ambientes**: Homologação e Produção

## 🏗️ Arquitetura

O projeto segue os princípios de **Clean Architecture** com as seguintes camadas:

```
src/main/java/br/com/healthcheck/
├── domain/              # Camada de Domínio (regras de negócio)
│   ├── entity/          # Entidades de domínio
│   ├── repository/     # Interfaces de repositório
│   └── usecase/         # Casos de uso
├── data/                # Camada de Dados
│   └── repository/      # Implementações de repositório
├── presentation/         # Camada de Apresentação
│   ├── dto/             # Data Transfer Objects
│   └── handler/          # Handlers HTTP
└── infrastructure/       # Camada de Infraestrutura
    ├── config/          # Configurações
    ├── server/          # Servidor HTTP
    └── util/            # Utilitários (JSON parser)
```

### Princípios Aplicados

- **Separação de Responsabilidades**: Cada camada tem uma responsabilidade específica
- **Dependency Inversion**: Camadas externas dependem de interfaces definidas nas camadas internas
- **Testabilidade**: Fácil de testar cada camada isoladamente
- **Manutenibilidade**: Código organizado e fácil de entender

## 📝 Notas

- O servidor roda na porta 3000 por padrão
- Certifique-se de que as URLs estão acessíveis (via VPN se necessário)
- Os arquivos de configuração podem ser editados enquanto o servidor está rodando, mas será necessário reiniciar para aplicar mudanças
- Se a porta 3000 estiver em uso, altere a constante `PORT` no arquivo `HealthCheckServer.java` e recompile
- O ambiente padrão ao iniciar o servidor é **Homologação**
- A troca de ambiente no dashboard é instantânea e não requer reiniciar o servidor

## 🚨 Troubleshooting

**Erro de conexão**: Verifique se o servidor está rodando e se as URLs estão acessíveis

**Timeout**: Aumente o valor de `timeout` no `config.json` se seus serviços demoram mais para responder

**CORS**: Se necessário, ajuste as configurações de CORS no `server.js`

## 📄 Licença

MIT

