#!/bin/bash
# Script para compilar e executar o servidor Health Check em Java

echo "☕ Health Check Server - Java"
echo "============================"
echo ""

# Verificar se Java está instalado
if ! command -v javac &> /dev/null; then
    echo "❌ Java não encontrado!"
    echo "   Por favor, instale o JDK (Java Development Kit) para continuar."
    exit 1
fi

# Verificar versão do Java
JAVA_VERSION=$(javac -version 2>&1)
echo "✅ $JAVA_VERSION encontrado"
echo ""

# Verificar se os arquivos de configuração existem
if [ ! -f "config-homolog.json" ]; then
    echo "⚠️  Arquivo config-homolog.json não encontrado!"
    echo "   Criando arquivo de exemplo..."
    cat > config-homolog.json << 'EOF'
{
  "refreshInterval": 30000,
  "timeout": 5000,
  "services": [
    {
      "name": "API Principal - Homolog",
      "url": "https://api-homolog.exemplo.com/health",
      "category": "Backend",
      "expectedStatus": 200
    }
  ]
}
EOF
    echo "✅ Arquivo config-homolog.json criado. Por favor, edite com suas URLs."
    echo ""
fi

if [ ! -f "config-prod.json" ]; then
    echo "⚠️  Arquivo config-prod.json não encontrado!"
    echo "   Criando arquivo de exemplo..."
    cat > config-prod.json << 'EOF'
{
  "refreshInterval": 30000,
  "timeout": 5000,
  "services": [
    {
      "name": "API Principal - Produção",
      "url": "https://api.exemplo.com/health",
      "category": "Backend",
      "expectedStatus": 200
    }
  ]
}
EOF
    echo "✅ Arquivo config-prod.json criado. Por favor, edite com suas URLs."
    echo ""
fi

# Verificar se a pasta public existe
if [ ! -d "public" ]; then
    echo "❌ Pasta 'public' não encontrada!"
    exit 1
fi

echo "🔨 Compilando servidor..."
javac HealthCheckServer.java

if [ $? -ne 0 ]; then
    echo "❌ Erro ao compilar!"
    exit 1
fi

echo "✅ Compilação concluída!"
echo ""
echo "🚀 Iniciando servidor..."
echo "📊 Dashboard estará disponível em: http://localhost:3000"
echo "📝 Pressione Ctrl+C para parar o servidor"
echo ""

# Executar o servidor
java HealthCheckServer

