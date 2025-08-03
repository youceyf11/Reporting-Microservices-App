#!/usr/bin/env bash
set -euo pipefail

# ───────── 0. Build & Up ─────────
export JIRA_MOCK=true              # active JiraMockConfig
docker compose up -d --build       # compile & (re)lance tous les services
sleep 5                            # petit temps pour que Spring démarre

# ───────── 1. Unit-Tests Reporting ─────────
echo "🧪  Tests unitaires Reporting-Service…"
docker compose run --rm reporting-service \
       mvn -q -pl reporting-service test
echo "✅  Unit tests OK"

# ───────── 2. Variables utiles ─────────
source .env                         # Récupère les ports
BASE_REP="http://localhost:${REPORTING_SERVICE_PORT}/api/reporting"
BASE_CHART="http://localhost:${CHART_SERVICE_PORT}/api/charts"
BASE_EMAIL="http://localhost:${EMAIL_SERVICE_PORT}/api/emails"
MAILHOG_API="http://localhost:8025/api/v2/messages"
PROJECT_KEY="SCRUM"
IMG_PATH="/tmp/chart.png"

# ───────── 3. Reporting ─────────
echo "➡️  Rapport mensuel $PROJECT_KEY"
curl -s "${BASE_REP}/monthly?projectKey=${PROJECT_KEY}" | jq .

# ───────── 4. Chart ─────────
echo "➡️  Chart mensuel"
curl -s "${BASE_CHART}/monthly/summary?projectKey=${PROJECT_KEY}" -o "${IMG_PATH}"
echo "   Chart sauvé → ${IMG_PATH}"

# ───────── 5. Email ─────────
echo "➡️  Envoi e-mail…"
curl -s -X POST "${BASE_EMAIL}/send/chart" \
     -F "request={\"to\":\"${MAIL_USERNAME}\",\"subject\":\"Chart ${PROJECT_KEY}\",\"templateData\":{\"projectKey\":\"${PROJECT_KEY}\",\"chartType\":\"monthly\"},\"priority\":\"LOW\"};type=application/json" \
     -F "file=@${IMG_PATH};type=image/png" | jq .

# ───────── 6. Attente & Vérification ─────────
echo "⏳  Attente 10 s"
sleep 10
echo "🔍  Vérif Mail…"
if curl -s "${MAILHOG_API}" | jq '.total' | grep -q '[1-9]'; then
  echo "✅  Mail reçu – Pipeline OK"
else
  echo "❌  Mail non trouvé"
  exit 1
fi
