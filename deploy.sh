#!/bin/bash

echo "🔧 Limpando build antigo..."
mvn clean

echo "📦 Compilando investment-advisor (production, skipTests)..."
mvn -Pproduction -DskipTests package

echo "🐳 Construindo imagem Docker..."
docker build . -t investment-advisor:1.0.0

echo "🐳 Subindo Docker Compose..."
docker compose up -d --build

echo "🎉 investment-advisor rodando!"
echo "👉 http://localhost:8085"