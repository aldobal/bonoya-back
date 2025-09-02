#!/bin/bash

# Script para probar la integración del Historial de Análisis
# Este script prueba todos los endpoints del backend

BASE_URL="http://localhost:8080/api/v1/inversor"
TOKEN="eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJpbnZlcnNpb25pc3RhIiwiaWF0IjoxNzUxMDgwMzAwLCJleHAiOjE3NTE2ODUxMDB9.RPTuJde2LpZ8pTlvqQTtN-_ytatG8PiyAEGs7fpML97N9FAyQmJ6IG7pw0Vc9Ds-"

echo "🚀 Probando Integración del Historial de Análisis"
echo "=================================================="

# 1. Probar GET /calculos (obtener historial)
echo ""
echo "1️⃣ Probando: GET /calculos (Obtener historial)"
echo "curl -X GET \"${BASE_URL}/calculos\" -H \"Authorization: Bearer TOKEN\""
curl -X GET "${BASE_URL}/calculos" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -s | jq '.' 2>/dev/null || echo "Response received (no jq installed)"

# 2. Probar GET /bonos/catalogo (para obtener bonos disponibles)
echo ""
echo "2️⃣ Probando: GET /bonos/catalogo (Obtener bonos disponibles)"
echo "curl -X GET \"${BASE_URL}/bonos/catalogo\""
curl -X GET "${BASE_URL}/bonos/catalogo" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -s | jq '.' 2>/dev/null || echo "Response received (no jq installed)"

# 3. Probar POST /calculos (crear nuevo análisis)
echo ""
echo "3️⃣ Probando: POST /calculos (Crear nuevo análisis)"
echo "curl -X POST \"${BASE_URL}/calculos\" -d '{\"bonoId\": 1, \"tasaEsperada\": 8.5}'"
curl -X POST "${BASE_URL}/calculos" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"bonoId": 1, "tasaEsperada": 8.5}' \
  -s | jq '.' 2>/dev/null || echo "Response received (no jq installed)"

echo ""
echo "✅ Pruebas completadas!"
echo ""
echo "📝 Nota: Si ves datos en las respuestas, la integración está funcionando correctamente."
echo "📝 Si hay errores 401, verificar el token de autenticación."
echo "📝 Si hay errores 404, verificar que el backend esté corriendo en puerto 8080."
